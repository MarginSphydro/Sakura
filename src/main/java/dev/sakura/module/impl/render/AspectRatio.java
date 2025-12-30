package dev.sakura.module.impl.render;


import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.values.impl.NumberValue;

public class AspectRatio extends Module {
    public AspectRatio() {
        super("AspectRatio", "画面比例", Category.Render);
    }

    public NumberValue<Double> ratio = new NumberValue<>("Ratio", "比例", 1.78, 0.0, 5.0, 0.01);
}
