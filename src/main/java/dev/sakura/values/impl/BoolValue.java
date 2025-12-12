package dev.sakura.values.impl;

import dev.sakura.values.Value;

/**
 * @Author：Guyuemang
 * @Date：2025/6/1 00:47
 */
public class BoolValue extends Value<Boolean> {
    public BoolValue(String name, boolean defaultValue, Dependency dependency) {
        super(name, dependency);
        this.value = defaultValue;
    }

    public BoolValue(String name, boolean defaultValue) {
        this(name, defaultValue, () -> true);
    }

    public void toggle() {
        value = !value;
    }
}