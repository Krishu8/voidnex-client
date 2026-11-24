package com.voidnex.client.module;

public enum Category {
    COMBAT("Combat"), MOVEMENT("Movement"), PLAYER("Player"), RENDER("Render"),
    WORLD("World"), UTILITY("Utility"), HUD("HUD"), MISC("Misc");

    public final String label;

    Category(String label) { this.label = label; }
}
