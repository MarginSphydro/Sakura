package dev.sakura.nanovg.font;

public class FontLoader {
    public static int greycliffRegular(float size) {
        return FontManager.fontWithCJK("regular.otf", size);
    }

    public static int greycliffBold(float size) {
        return FontManager.fontWithCJK("regular_bold.otf", size);
    }

    public static int greycliffMedium(float size) {
        return FontManager.fontWithCJK("regular_medium.otf", size);
    }

    public static int greycliffSemi(float size) {
        return FontManager.fontWithCJK("regular_semi.otf", size);
    }

    public static int icon(float size) {
        return FontManager.font("solid.ttf", size);
    }

    public static int icons(float size) {
        return FontManager.font("icon.ttf", size);
    }

    public static int cjk(float size) {
        return FontManager.font("Kuriyama.ttf", size);
    }
}
