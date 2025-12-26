package dev.sakura.module.impl.client;

import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.values.impl.StringValue;

public class StringTest extends Module {
    public StringTest() {
        super("StringTest", "字符串测试", Category.Client);
    }

    private final StringValue test = new StringValue("Nihao", "Yes");
}
