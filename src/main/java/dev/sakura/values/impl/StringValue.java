package dev.sakura.values.impl;

import dev.sakura.values.Value;

public class StringValue extends Value<String> {
    private boolean onlyNumber;

    public StringValue(String name, String defaultValue, Dependency dependency) {
        super(name, dependency);
        this.value = defaultValue;
        this.onlyNumber = false;
    }

    public StringValue(String name, String defaultValue) {
        super(name, () -> true);
        this.value = defaultValue;
        this.onlyNumber = false;
    }

    public StringValue(String name, String defaultValue, boolean onlyNumber, Dependency dependency) {
        super(name, dependency);
        this.value = defaultValue;
        this.onlyNumber = onlyNumber;
    }

    public StringValue(String name, String defaultValue, boolean onlyNumber) {
        super(name, () -> true);
        this.value = defaultValue;
        this.onlyNumber = onlyNumber;
    }

    public String getText() {
        return get();
    }

    public boolean isOnlyNumber() {
        return onlyNumber;
    }

    public void setText(String text) {
        set(text);
    }

    public void setOnlyNumber(boolean onlyNumber) {
        this.onlyNumber = onlyNumber;
    }
}
