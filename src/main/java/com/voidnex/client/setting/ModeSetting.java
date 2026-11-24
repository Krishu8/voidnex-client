package com.voidnex.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Dropdown setting. The first option is the default. */
public class ModeSetting extends Setting<String> {
    public final String[] options;
    private int index;

    public ModeSetting(String name, String description, String... options) {
        super(name, description, options[0]);
        this.options = options;
    }

    @Override public void set(String v) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(v)) {
                index = i;
                value = options[i];
                return;
            }
        }
    }

    public int index() { return index; }
    public boolean is(String option) { return value.equals(option); }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { set(e.getAsString()); }
}
