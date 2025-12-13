package dev.sakura.module;

import dev.sakura.Sakura;
import dev.sakura.events.input.MouseButtonEvent;
import dev.sakura.events.misc.KeyAction;
import dev.sakura.events.misc.KeyEvent;
import dev.sakura.events.render.Render2DEvent;
import dev.sakura.manager.Managers;
import dev.sakura.manager.NotificationManager;
import dev.sakura.manager.SoundManager;
import dev.sakura.module.impl.client.ClickGui;
import dev.sakura.module.impl.client.HudEditor;
import dev.sakura.module.impl.client.StringTest;
import dev.sakura.module.impl.combat.AntiKnockback;
import dev.sakura.module.impl.combat.Burrow;
import dev.sakura.module.impl.hud.*;
import dev.sakura.module.impl.movement.AutoSprint;
import dev.sakura.module.impl.render.CameraClip;
import dev.sakura.module.impl.render.Fullbright;
import dev.sakura.module.impl.render.SwingAnimation;
import dev.sakura.module.impl.render.ViewModel;
import dev.sakura.values.Value;
import meteordevelopment.orbit.EventHandler;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author：Gu-Yuemang
 * @Date：2025/11/14 01:21
 */
public class ModuleManager {
    private final Map<Class<? extends Module>, Module> modules;

    public ModuleManager() {
        modules = new LinkedHashMap<>();
    }

    public void Init() {
        Sakura.EVENT_BUS.subscribe(this);
        // Combat
        addModule(new AntiKnockback());
        addModule(new Burrow());


        // Movement
        addModule(new AutoSprint());

        // Render
        addModule(new CameraClip());
        addModule(new Fullbright());
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
    public void onKey(KeyEvent e) {
        if (e.getAction() == KeyAction.Press) {
            for (Module module : modules.values()) {
                if (module.getKey() == e.getKey()) {
                    if (module.isDisabled()) {
                        NotificationManager.send(module.hashCode(), "§7" + module.getName() + "§a enabled", 3000L);
                        SoundManager.playSound(SoundManager.ENABLE);
                    } else {
                        NotificationManager.send(module.hashCode(), "§7" + module.getName() + "§c disabled", 3000L);
                        SoundManager.playSound(SoundManager.DISABLE);
                    }
                    module.toggle();
                }
            }
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
