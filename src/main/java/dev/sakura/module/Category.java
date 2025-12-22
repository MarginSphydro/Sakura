package dev.sakura.module;

public enum Category {
    Combat("A"),
    Movement("C"),
    Player("B"),
    Render("M"),
    Client("D");

    public final String icon;

    Category(String icon) {
        this.icon = icon;
    }
}