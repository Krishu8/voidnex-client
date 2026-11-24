package com.voidnex.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public class NumberSetting extends Setting<Double> {
    public final double min, max, step;

    public NumberSetting(String name, String description, double def, double min, double max, double step) {
        super(name, description, def);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override public void set(Double v) { set(v.doubleValue()); }

    public void set(double v) {
        double x = Math.max(min, Math.min(max, v));
        if (step > 0) x = Math.round((x - min) / step) * step + min;
        value = Math.max(min, Math.min(max, x));
    }

    public double d() { return value; }
    public int i() { return (int) Math.round(value); }
    public float f() { return value.floatValue(); }

    public double progress() { return (value - min) / (max - min); }
    public void setProgress(double p) { set(min + (max - min) * Math.max(0, Math.min(1, p))); }

    public String display() {
        return step >= 1 ? String.valueOf(i()) : String.format(Locale.ROOT, "%.2f", value);
    }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { set(e.getAsDouble()); }
}
