package dev.sakura.module;

import dev.sakura.Sakura;
import dev.sakura.utils.animations.Animation;
import dev.sakura.utils.animations.Direction;
import dev.sakura.utils.animations.impl.DecelerateAnimation;
import dev.sakura.values.Value;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author：Gu-Yuemang
 * @Date：2025/11/14 01:18
 */
public class Module {
    private final String name;
    private boolean state;
    private final Category category;
    private String suffix = "";
    private int key;
    public final List<Value<?>> values = new ArrayList<>();
    private final Animation animations = new DecelerateAnimation(250, 1).setDirection(Direction.BACKWARDS);

    protected final MinecraftClient mc;

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
        this.mc = MinecraftClient.getInstance();
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public boolean isEnabled() {
        return state;
    }

    public boolean isDisabled() {
        return !state;
    }

    public <M extends Module> boolean isEnabled(Class<M> module) {
        Module mod = Sakura.MODULE.getModule(module);
        return mod != null && mod.isEnabled();
    }

    public <M extends Module> boolean isDisabled(Class<M> module) {
        Module mod = Sakura.MODULE.getModule(module);
        return mod.isDisabled();
    }

    public void setSuffix(String tag) {
        if (tag != null && !tag.isEmpty()) {
            //ModuleList arrayListModule = Alisa.moduleManager.getModule(ModuleList.class);

//            if (arrayListModule != null) {
//                String tagStyle = arrayListModule.tags.get().toLowerCase();
//                switch (tagStyle) {
//                    case "simple":
//                        this.suffix = " " + tag + "";
//                        break;
//                    case "dash":
//                        this.suffix = " - " + tag + "";
//                        break;
//                    case "bracket":
//                        this.suffix = " [" + tag + "]" + "";
//                        break;
//                    default:
//                        this.suffix = "";
//                }
//            } else {
//                this.suffix = " " + tag;
//            }

            this.suffix = " " + tag;
        } else {
            this.suffix = "";
        }
    }

    public void toggle() {
        setState(!state);
    }

    public void setState(boolean state) {
        if (this.state != state) {
            this.state = state;
            if (state) {
                Sakura.EVENT_BUS.subscribe(this);
                onEnable();
            } else {
                Sakura.EVENT_BUS.unsubscribe(this);
                onDisable();
            }
        }
    }

    public String getName() {
        return name;
    }

    public boolean isState() {
        return state;
    }

    public Category getCategory() {
        return category;
    }

    public String getSuffix() {
        return suffix;
    }

    public int getKey() {
        return key;
    }

    public List<Value<?>> getValues() {
        return values;
    }

    public Animation getAnimations() {
        return animations;
    }

    public void setKey(int key) {
        this.key = key;
    }
}
