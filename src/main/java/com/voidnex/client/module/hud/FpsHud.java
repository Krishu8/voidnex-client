package com.voidnex.client.module.hud;

import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.BoolSetting;

import java.util.List;

public class FpsHud extends TextHud {
    private final BoolSetting label = add(new BoolSetting("Show Label", "Prefix the value with 'FPS'.", true));
    private final BoolSetting dynamic = add(new BoolSetting("Dynamic Color", "Color by performance: green, yellow, red.", false));

    public FpsHud() {
        super("FPS", "Shows your frames per second.", 0.01f, 0.02f);
        defaultEnabled();
    }

    @Override protected List<String> lines(boolean preview) {
        int f = mc.getFps();
        return List.of(label.on() ? "FPS: " + f : String.valueOf(f));
    }

    @Override protected int colorFor(int line) {
        if (!dynamic.on()) return super.colorFor(line);
        int f = mc.getFps();
        return f >= 60 ? 0x55FF55 : f >= 30 ? 0xFFFF55 : 0xFF5555;
    }
}
