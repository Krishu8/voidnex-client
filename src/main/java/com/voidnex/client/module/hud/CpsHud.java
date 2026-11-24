package com.voidnex.client.module.hud;

import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.util.Cps;

import java.util.List;

public class CpsHud extends TextHud {
    private final BoolSetting right = add(new BoolSetting("Show Right Click", "Also show right-click CPS.", true));
    private final BoolSetting label = add(new BoolSetting("Show Label", "Prefix the value with 'CPS'.", true));

    public CpsHud() {
        super("CPS", "Shows your clicks per second.", 0.01f, 0.075f);
        defaultEnabled();
    }

    @Override protected List<String> lines(boolean preview) {
        String v = right.on() ? Cps.left() + " | " + Cps.right() : String.valueOf(Cps.left());
        return List.of(label.on() ? "CPS: " + v : v);
    }
}
