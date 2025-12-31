package dev.sakura.loader.agent;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentLoader {
    private static boolean loaded = false;

    private static final String CLOUD_CLASSES_KEY = "sakura.cloud.classes";

    @SuppressWarnings("unchecked")
    public static Map<String, byte[]> getCloudClasses() {
        Object obj = System.getProperties().get(CLOUD_CLASSES_KEY);
        if (obj == null) {
            Map<String, byte[]> map = new ConcurrentHashMap<>();
            System.getProperties().put(CLOUD_CLASSES_KEY, map);
            return map;
        }
        return (Map<String, byte[]>) obj;
    }

    public static void registerCloudClass(String className, byte[] bytes) {
        Map<String, byte[]> cloudClasses = getCloudClasses();
        cloudClasses.put(className, bytes);
        cloudClasses.put(className.replace('.', '/'), bytes);
    }

    public static void loadAgent() {
        if (loaded) {
            return;
        }

        try {
            Instrumentation instrumentation = ByteBuddyAgent.install();

            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.named("net.fabricmc.loader.impl.launch.knot.KnotClassDelegate"))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                            builder
                                    .visit(Advice.to(GRCBAdvice.class)
                                            .on(ElementMatchers.named("getRawClassBytes")
                                                    .and(ElementMatchers.takesArguments(1))
                                                    .and(ElementMatchers.returns(byte[].class))))
                                    .visit(Advice.to(GRCBAdvice.class)
                                            .on(ElementMatchers.named("getRawClassByteArray")
                                                    .and(ElementMatchers.takesArguments(2))
                                                    .and(ElementMatchers.returns(byte[].class))))
                                    .visit(Advice.to(GRCBAdvice.class)
                                            .on(ElementMatchers.named("getPreMixinClassBytes")
                                                    .and(ElementMatchers.takesArguments(1))
                                                    .and(ElementMatchers.returns(byte[].class))))
                                    .visit(Advice.to(GRCBAdvice.class)
                                            .on(ElementMatchers.named("getPreMixinClassByteArray")
                                                    .and(ElementMatchers.takesArguments(2))
                                                    .and(ElementMatchers.returns(byte[].class)))))
                    .installOn(instrumentation);

            loaded = true;

        } catch (Exception e) {
            System.err.println("[AgentLoader] Failed to load agent: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
