package com.voidnex.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, boolean def) { super(name, description, def); }

    public boolean on() { return value; }
    public void toggle() { value = !value; }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { value = e.getAsBoolean(); }
}
