package com.voidnex.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class StringSetting extends Setting<String> {
    public final int max;

    public StringSetting(String name, String description, String def, int max) {
        super(name, description, def);
        this.max = max;
    }

    @Override public void set(String v) { value = v.length() > max ? v.substring(0, max) : v; }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { set(e.getAsString()); }
}
