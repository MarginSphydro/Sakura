package dev.sakura.values.impl;

import dev.sakura.utils.color.ColorUtil;
import dev.sakura.values.Value;

import java.awt.*;

public class ColorValue extends Value<Color> {
    private float hue = 0;
    private float saturation = 1;
    private float brightness = 1;
    private float alpha = 1;
    private boolean allowAlpha = false;
    private boolean rainbow = false;
    public boolean expand;

    public ColorValue(String name, Color defaultValue, boolean allowAlpha, Dependency dependency) {
        super(name, dependency);
        this.allowAlpha = allowAlpha;
        set(defaultValue);
    }

    public ColorValue(String name, Color defaultValue, Dependency dependency) {
        this(name, defaultValue, true, dependency);
    }

    public ColorValue(String name, Color defaultValue) {
        this(name, defaultValue, true, () -> true);
    }

    public Color get() {
        return ColorUtil.applyOpacity(Color.getHSBColor(hue, saturation, brightness), alpha);
    }

    public void set(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        alpha = color.getAlpha() / 255.0f;
    }

    public boolean allowAlpha() {
        return allowAlpha;
    }

    public float getHue() {
        return hue;
    }

    public float getSaturation() {
        return saturation;
    }

    public float getBrightness() {
        return brightness;
    }

    public float getAlpha() {
        return alpha;
    }

    public boolean isRainbow() {
        return rainbow;
    }

    public boolean isExpand() {
        return expand;
    }

    public void setHue(float hue) {
        this.hue = hue;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }

    public void setExpand(boolean expand) {
        this.expand = expand;
    }
}