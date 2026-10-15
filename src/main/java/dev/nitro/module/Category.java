package dev.nitro.module;

public enum Category {
    COMBAT("Combat"), RENDER("Render"), WORLD("World"), MISC("Misc");

    public final String display;
    Category(String display) { this.display = display; }
}
