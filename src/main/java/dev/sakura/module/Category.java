package dev.sakura.module;

public enum Category {
    Combat("A"),
    Player("B"),
    Movement("C"),
    Render("M"),
    Client("D"),
    HUD("F");

    public final String icon;

    Category(String icon) {
        this.icon = icon;
    }
}
