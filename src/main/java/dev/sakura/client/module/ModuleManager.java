package dev.sakura.client.module;

import dev.sakura.client.Sakura;
import dev.sakura.client.events.input.MouseButtonEvent;
import dev.sakura.client.events.misc.KeyAction;
import dev.sakura.client.events.misc.KeyEvent;
import dev.sakura.client.events.render.Render2DEvent;
import dev.sakura.client.manager.impl.NotificationManager;
import dev.sakura.client.manager.impl.SoundManager;
import dev.sakura.client.module.impl.client.ClickGui;
import dev.sakura.client.module.impl.client.HudEditor;
import dev.sakura.client.module.impl.combat.*;
import dev.sakura.client.module.impl.hud.*;
import dev.sakura.client.module.impl.movement.*;
import dev.sakura.client.module.impl.movement.velocity.Velocity;
import dev.sakura.client.module.impl.player.*;
import dev.sakura.client.module.impl.render.*;
import dev.sakura.client.values.Value;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static dev.sakura.client.Sakura.mc;

public class ModuleManager {
    private final Map<Class<? extends Module>, Module> modules;

    public ModuleManager() {
        modules = new LinkedHashMap<>();
        Sakura.EVENT_BUS.subscribe(this);

        try {
            ManualLoader.load(this);
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("Convert2MethodRef")
    private static class ManualLoader {
        static void load(ModuleManager manager) {
            // Combat
            manager.tryLoad(() -> new SelfTrap());
            manager.tryLoad(() -> new AutoTrap());
            manager.tryLoad(() -> new AnchorAura());
            manager.tryLoad(() -> new AutoPot());
            manager.tryLoad(() -> new Velocity());
            manager.tryLoad(() -> new AutoTotem());
            manager.tryLoad(() -> new CrystalAura());
            manager.tryLoad(() -> new Burrow());
            manager.tryLoad(() -> new KillAura());
            manager.tryLoad(() -> new Surround());
            manager.tryLoad(() -> new WebAura());

            // Movement
            manager.tryLoad(() -> new AutoSprint());
            manager.tryLoad(() -> new ElytraBoost());
            manager.tryLoad(() -> new Speed());
            manager.tryLoad(() -> new Step());
            manager.tryLoad(() -> new NoSlow());
            manager.tryLoad(() -> new HoleSnap());
            manager.tryLoad(() -> new NoFall());
            manager.tryLoad(() -> new Scaffold());
            manager.tryLoad(() -> new ElytraFly());


            // Player
            manager.tryLoad(() -> new NoRotate());
            manager.tryLoad(() -> new FakePlayer());
            manager.tryLoad(() -> new AutoPearl());
            manager.tryLoad(() -> new TimerModule());
            manager.tryLoad(() -> new PacketEat());
            manager.tryLoad(() -> new InventorySort());
            manager.tryLoad(() -> new AutoArmor());
            manager.tryLoad(() -> new ArmorFly());
            manager.tryLoad(() -> new Blink());

            // Render
            manager.tryLoad(() -> new TargetESP());
            manager.tryLoad(() -> new CameraClip());
            manager.tryLoad(() -> new Fullbright());
            manager.tryLoad(() -> new NoRender());
            manager.tryLoad(() -> new SwingAnimation());
            manager.tryLoad(() -> new ViewModel());
            manager.tryLoad(() -> new Hat());
            manager.tryLoad(() -> new JumpCircles());
            manager.tryLoad(() -> new XRay());
            manager.tryLoad(() -> new NameTags());
            manager.tryLoad(() -> new Glow());
            manager.tryLoad(() -> new TotemParticles());
            manager.tryLoad(() -> new Atmosphere());
            manager.tryLoad(() -> new AspectRatio());
            manager.tryLoad(() -> new Crystal());

            // Client
            manager.tryLoad(() -> new ClickGui());
            manager.tryLoad(() -> new HudEditor());

            // HUD
            manager.tryLoad(() -> new FPSHud());
            manager.tryLoad(() -> new NotificationHud());
            manager.tryLoad(() -> new WatermarkHud());
            manager.tryLoad(() -> new MSHud());
            manager.tryLoad(() -> new DynamicIslandHud());
            manager.tryLoad(() -> new KeyStrokesHud());
            manager.tryLoad(() -> new NotifyHud());
            manager.tryLoad(() -> new HotbarHud());
            manager.tryLoad(() -> new ModuleListHud());
            manager.tryLoad(() -> new TargetHud());
        }
    }

    private void tryLoad(Supplier<Module> supplier) {
        try {
            addModule(supplier.get());
        } catch (NoClassDefFoundError | Exception ignored) {
        }
    }

    private void addModule(Module module) {
        for (final Field field : module.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                final Object obj = field.get(module);
                if (obj instanceof Value<?>) module.getValues().add((Value<?>) obj);
            } catch (IllegalAccessException ignored) {
            }
        }
        modules.put(module.getClass(), module);
    }

    public Collection<Module> getAllModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    public Module getModuleByString(String name) {
        for (Module module : modules.values()) {
            if (module.getEnglishName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public <T extends Module> T getModule(Class<T> cls) {
        return cls.cast(modules.get(cls));
    }

    public List<Module> getModsByCategory(Category m) {
        return modules.values().stream()
                .filter(module -> module.getCategory() == m)
                .sorted(Comparator.comparing(Module::getEnglishName))
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (mc.currentScreen != null) return;

        boolean isPress = event.getAction() == KeyAction.Press;
        boolean isRelease = event.getAction() == KeyAction.Release;

        List<Module> affectedModules = new ArrayList<>();
        boolean hasEnabling = false;

        for (Module module : modules.values()) {
            if (module.getKey() != event.getKey()) continue;

            if (module.getBindMode() == Module.BindMode.Toggle && isPress) {
                if (!module.isEnabled()) hasEnabling = true;
                affectedModules.add(module);
            } else if (module.getBindMode() == Module.BindMode.Hold) {
                if (isPress && !module.isEnabled()) {
                    hasEnabling = true;
                    affectedModules.add(module);
                } else if (isRelease && module.isEnabled()) {
                    affectedModules.add(module);
                }
            }
        }

        for (Module module : affectedModules) {
            if (module.getBindMode() == Module.BindMode.Toggle) {
                boolean enabling = !module.isEnabled();
                sendToggleNotification(module, enabling, "", false);
                module.toggle();
            } else if (module.getBindMode() == Module.BindMode.Hold) {
                if (isPress && !module.isEnabled()) {
                    sendToggleNotification(module, true, " §8(Hold)", false);
                    module.setState(true);
                } else if (isRelease && module.isEnabled()) {
                    sendToggleNotification(module, false, "", false);
                    module.setState(false);
                }
            }
        }

        if (!affectedModules.isEmpty()) {
            if (hasEnabling) {
                SoundManager.playSound(SoundManager.ENABLE);
            } else {
                SoundManager.playSound(SoundManager.DISABLE);
            }
        }
    }

    private void sendToggleNotification(Module module, boolean enabling) {
        sendToggleNotification(module, enabling, "", true);
    }

    private void sendToggleNotification(Module module, boolean enabling, String suffix, boolean playSound) {
        String name = module.getDisplayName();
        String status;
        if (ClickGui.language.get() == ClickGui.Language.Chinese) {
            status = enabling ? "§a 已开启" : "§c 已关闭";
        } else {
            status = enabling ? "§a enabled" : "§c disabled";
        }
        NotificationManager.send(module.hashCode(), "§7" + name + status + suffix, 3000L);
        if (playSound) {
            SoundManager.playSound(enabling ? SoundManager.ENABLE : SoundManager.DISABLE);
        }
    }

    @EventHandler
    public void onKey(MouseButtonEvent e) {
        if (e.getAction() == KeyAction.Press) {
            if (e.getButton() == 3 || e.getButton() == 4) {
                for (Module module : modules.values()) {
                    if (module.getKey() == -e.getButton()) {
                        module.toggle();
                    }
                }
            }
        }
    }

    public Collection<HudModule> getAllHudModules() {
        return modules.values().stream()
                .filter(HudModule.class::isInstance)
                .map(HudModule.class::cast)
                .collect(Collectors.toList());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRender2D(Render2DEvent event) {
        for (HudModule module : getAllHudModules()) {
            if (module.isState()) {
                module.renderInGame(event.getContext());
            }
        }
    }
}