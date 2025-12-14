package dev.sakura.module;

import dev.sakura.Sakura;
import dev.sakura.events.input.MouseButtonEvent;
import dev.sakura.events.misc.KeyAction;
import dev.sakura.events.misc.KeyEvent;
import dev.sakura.events.render.Render2DEvent;
import dev.sakura.manager.NotificationManager;
import dev.sakura.manager.SoundManager;
import dev.sakura.module.impl.client.ClickGui;
import dev.sakura.module.impl.client.HudEditor;
import dev.sakura.module.impl.client.StringTest;
import dev.sakura.module.impl.combat.Burrow;
import dev.sakura.module.impl.combat.Velocity;
import dev.sakura.module.impl.hud.*;
import dev.sakura.module.impl.movement.AutoSprint;
import dev.sakura.module.impl.movement.NoSlow;
import dev.sakura.module.impl.movement.Speed;
import dev.sakura.module.impl.movement.Step;
import dev.sakura.module.impl.render.*;
import dev.sakura.gui.dropdown.ClickGuiScreen;
import dev.sakura.gui.hud.HudEditorScreen;
import dev.sakura.values.Value;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.*;
import java.util.stream.Collectors;

public class ModuleManager {
    private final Map<Class<? extends Module>, Module> modules;

    public ModuleManager() {
        modules = new LinkedHashMap<>();
    }

    public void Init() {
        Sakura.EVENT_BUS.subscribe(this);

        // Combat
        addModule(new Burrow());
        addModule(new Velocity());


        // Movement
        addModule(new AutoSprint());
        addModule(new Speed());
        addModule(new Step());
        addModule(new NoSlow());

        // Render
        addModule(new CameraClip());
        addModule(new Fullbright());
        addModule(new NoRender());
        addModule(new SwingAnimation());
        addModule(new ViewModel());

        // Client
        addModule(new ClickGui());
        addModule(new HudEditor());
        addModule(new StringTest());

        // HUD
        addModule(new FPSHud());
        addModule(new NotificationHud());
        addModule(new WatermarkHud());
        addModule(new MSHud());
        addModule(new DynamicIslandHud());
    }

    public void addModule(Module module) {
        for (final Field field : module.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                final Object obj = field.get(module);
                if (obj instanceof Value<?>) module.getValues().add((Value<?>) obj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        modules.put(module.getClass(), module);
    }

    public Collection<Module> getAllModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    public Module getModule(String name) {
        for (Module module : modules.values()) {
            if (module.getName().equalsIgnoreCase(name)) {
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
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        Screen currentScreen = Sakura.mc.currentScreen;
        if (currentScreen instanceof ClickGuiScreen || currentScreen instanceof HudEditorScreen) {
            return;
        }

        boolean isPress = event.getAction() == KeyAction.Press;
        boolean isRelease = event.getAction() == KeyAction.Release;

        List<Module> affectedModules = new ArrayList<>();
        boolean hasEnabling = false;

        for (Module module : modules.values()) {
            if (module.getKey() != event.getKey()) continue;

            if (module.getBindMode() == Module.BindMode.Toggle && isPress) {
                if (module.isDisabled()) hasEnabling = true;
                affectedModules.add(module);
            } else if (module.getBindMode() == Module.BindMode.Hold) {
                if (isPress && module.isDisabled()) {
                    hasEnabling = true;
                    affectedModules.add(module);
                } else if (isRelease && module.isEnabled()) {
                    affectedModules.add(module);
                }
            }
        }

        for (Module module : affectedModules) {
            if (module.getBindMode() == Module.BindMode.Toggle) {
                boolean enabling = module.isDisabled();
                sendToggleNotification(module, enabling, "", false);
                module.toggle();
            } else if (module.getBindMode() == Module.BindMode.Hold) {
                if (isPress && module.isDisabled()) {
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
        NotificationManager.send(module.hashCode(), "§7" + module.getName() + (enabling ? "§a enabled" : "§c disabled") + suffix, 3000L);
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

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        for (HudModule module : getAllHudModules()) {
            if (module.isState()) {
                module.renderInGame(event.getContext());
            }
        }
    }
}
