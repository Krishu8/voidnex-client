package com.voidnex.client.module.hud;

import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.util.Info;

import java.util.List;

public class PingHud extends TextHud {
    private final BoolSetting dynamic = add(new BoolSetting("Dynamic Color", "Color by latency: green, yellow, red.", true));

    public PingHud() {
        super("Ping", "Shows your latency to the server.", 0.01f, 0.13f);
    }

    @Override protected List<String> lines(boolean preview) {
        int p = Info.ping();
        return List.of("Ping: " + (p < 0 ? "--" : p + " ms"));
    }

    @Override protected int colorFor(int line) {
        if (!dynamic.on()) return super.colorFor(line);
        int p = Info.ping();
        return p < 0 ? super.colorFor(line) : p <= 80 ? 0x55FF55 : p <= 160 ? 0xFFFF55 : 0xFF5555;
    }
}
