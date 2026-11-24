package com.voidnex.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.Mth;

/** RGB color with an HSV editor (hue / saturation / brightness sliders in the UI). */
public class ColorSetting extends Setting<Integer> {
    private float h, s, v;

    public ColorSetting(String name, String description, int rgb) {
        super(name, description, rgb & 0xFFFFFF);
        sync();
    }

    @Override public void set(Integer rgb) { set(rgb.intValue()); }

    public void set(int rgb) {
        value = rgb & 0xFFFFFF;
        sync();
    }

    public int argb() { return 0xFF000000 | value; }
    public float h() { return h; }
    public float s() { return s; }
    public float v() { return v; }

    public void setHsv(float h, float s, float v) {
        this.h = h;
        this.s = s;
        this.v = v;
        value = Mth.hsvToRgb(h, s, v) & 0xFFFFFF;
    }

    private void sync() {
        float r = ((value >> 16) & 255) / 255f, g = ((value >> 8) & 255) / 255f, b = (value & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float nh = h;
        if (d != 0) {
            if (max == r) nh = (((g - b) / d) % 6f) / 6f;
            else if (max == g) nh = ((b - r) / d + 2f) / 6f;
            else nh = ((r - g) / d + 4f) / 6f;
            if (nh < 0) nh += 1f;
        }
        h = nh;
        s = max == 0 ? 0 : d / max;
        v = max;
    }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { set(e.getAsInt()); }
}
