package com.voidnex.client.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** Base class for every configurable value of a module or of the client itself. */
public abstract class Setting<T> {
    public final String name;
    public final String description;
    protected T value;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, String description, T def) {
        this.name = name;
        this.description = description;
        this.value = def;
    }

    public T get() { return value; }
    public void set(T v) { value = v; }

    /** Hide this setting in the UI unless the condition holds. */
    public Setting<T> showIf(BooleanSupplier condition) { visible = condition; return this; }
    public boolean isVisible() { return visible.getAsBoolean(); }

    public abstract JsonElement save();
    public abstract void load(JsonElement e);
}
