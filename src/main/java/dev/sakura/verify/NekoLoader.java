package dev.sakura.verify;

import dev.sakura.verify.agent.AgentLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import sun.misc.Unsafe;

import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NekoLoader {
    private static NekoLoader INSTANCE;
    private static final Map<String, byte[]> cloudClasses = new HashMap<>();
    private static boolean injected = false;

    public static NekoLoader getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NekoLoader();
        }
        return INSTANCE;
    }

    public void loadFromJar(byte[] jarBytes) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    byte[] bytes = zis.readAllBytes();
                    String className = new ClassReader(bytes).getClassName().replace("/", ".");
                    cloudClasses.put(className, bytes);
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            System.err.println("[NekoLoader] Failed to read JAR: " + e.getMessage());
            e.printStackTrace();
            return;
        }


        if (!injected) {
            injectIntoKnot();
            injected = true;
        }
    }

    @SuppressWarnings("all")
    private void injectIntoKnot() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);

            hookKnotClassDelegate(classLoader, unsafe);

            hookMixinBytecodeProvider(unsafe);

            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            long offset = unsafe.staticFieldOffset(implLookupField);
            MethodHandles.Lookup trustedLookup = (MethodHandles.Lookup) unsafe.getObject(
                    MethodHandles.Lookup.class, offset
            );

            MethodHandle defineClassMH = trustedLookup.findVirtual(
                    ClassLoader.class,
                    "defineClass",
                    MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class)
            );

            MethodHandle findLoadedClassMH = trustedLookup.findVirtual(
                    ClassLoader.class,
                    "findLoadedClass",
                    MethodType.methodType(Class.class, String.class)
            );

            Map<String, byte[]> pending = new HashMap<>();
            for (Map.Entry<String, byte[]> entry : cloudClasses.entrySet()) {
                String className = entry.getKey();
                byte[] bytes = entry.getValue();

                // Mixin类只通过Agent hook提供字节码，不直接定义
                if (className.contains(".mixin.")) {
                    continue;
                }

                // 检查父类，如果继承自Minecraft类则跳过（会触发Minecraft类的加载）
                ClassReader cr = new ClassReader(bytes);
                String superName = cr.getSuperName();
                if (superName != null && superName.startsWith("net/minecraft/")) {
                    continue;
                }

                // 检查接口，如果实现了Minecraft接口则跳过
                boolean hasMinecraftInterface = false;
                for (String iface : cr.getInterfaces()) {
                    if (iface.startsWith("net/minecraft/")) {
                        hasMinecraftInterface = true;
                        break;
                    }
                }
                if (hasMinecraftInterface) {
                    continue;
                }

                pending.put(className, bytes);
            }

            int maxAttempts = 10;
            int attempt = 0;

            while (!pending.isEmpty() && attempt < maxAttempts) {
                attempt++;
                Map<String, byte[]> failed = new HashMap<>();

                for (Map.Entry<String, byte[]> entry : pending.entrySet()) {
                    String className = entry.getKey();
                    byte[] bytes = entry.getValue();

                    try {
                        // 使用 findLoadedClass 检查，不会触发类加载
                        Object loaded = findLoadedClassMH.invoke(classLoader, className);
                        if (loaded == null) {
                            defineClassMH.invoke(classLoader, className, bytes, 0, bytes.length, null);
                        }
                    } catch (Throwable ex) {
                        failed.put(className, bytes);
                        if (attempt == maxAttempts) {
                            System.err.println("[NekoLoader] Failed to define: " + className + " - " + ex.getMessage());
                        }
                    }
                }

                pending = failed;
            }


        } catch (Exception e) {
            System.err.println("[NekoLoader] Failed to inject: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static byte[] getCloudClassBytes(String className) {
        return cloudClasses.get(className);
    }

    public static boolean hasCloudClass(String className) {
        return cloudClasses.containsKey(className);
    }

    private void hookKnotClassDelegate(ClassLoader classLoader, Unsafe unsafe) {
        try {
            for (Map.Entry<String, byte[]> entry : cloudClasses.entrySet()) {
                AgentLoader.registerCloudClass(entry.getKey(), entry.getValue());
            }
            AgentLoader.loadAgent();
        } catch (Exception e) {
            System.err.println("[NekoLoader] Failed to load agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void hookMixinBytecodeProvider(Unsafe unsafe) {
        try {
            Class<?> classInfoClass = Class.forName("org.spongepowered.asm.mixin.transformer.ClassInfo");

            Field cacheField = classInfoClass.getDeclaredField("cache");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> cache = (Map<String, Object>) cacheField.get(null);

            java.lang.reflect.Method forNameMethod = classInfoClass.getDeclaredMethod("forName", String.class);
            forNameMethod.setAccessible(true);

            java.lang.reflect.Method fromClassNodeMethod = null;
            for (java.lang.reflect.Method m : classInfoClass.getDeclaredMethods()) {
                if (m.getName().equals("fromClassNode")) {
                    fromClassNodeMethod = m;
                    break;
                }
            }

            if (fromClassNodeMethod != null) {
                fromClassNodeMethod.setAccessible(true);

                for (Map.Entry<String, byte[]> entry : cloudClasses.entrySet()) {
                    String className = entry.getKey();
                    byte[] bytes = entry.getValue();
                    String internalName = className.replace('.', '/');

                    // 跳过Mixin类，它们由Mixin系统自己处理
                    if (className.contains(".mixin.")) {
                        continue;
                    }

                    // 跳过继承自Minecraft类的云端类，避免触发Minecraft类加载
                    ClassReader cr = new ClassReader(bytes);
                    String superName = cr.getSuperName();
                    if (superName != null && superName.startsWith("net/minecraft/")) {
                        continue;
                    }

                    if (!cache.containsKey(internalName)) {
                        try {
                            ClassNode node = new ClassNode();
                            cr.accept(node, 0);

                            Object classInfo = fromClassNodeMethod.invoke(null, node);
                            cache.put(internalName, classInfo);
                        } catch (Exception e) {
                            System.err.println("[NekoLoader] Failed to create ClassInfo for: " + className + " - " + e.getMessage());
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[NekoLoader] Failed to hook ClassInfo cache: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
