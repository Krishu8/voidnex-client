package com.voidnex.client.module.world;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.BoolSetting;

import java.util.List;

/**
 * Shows the in-game time of day. Minecraft 26.1+ moved time into "world clocks",
 * so this reads the dimension's default clock (read-only, nothing is modified).
 */
public class WorldClock extends TextHud {
    private final BoolSetting hour24 = add(new BoolSetting("24 Hour", "Use a 24-hour clock.", true));
    private final BoolSetting phase = add(new BoolSetting("Show Day Phase", "Show Day / Sunset / Night / Sunrise.", true));

    public WorldClock() {
        super("World Clock", "In-game time of day.", Category.WORLD, 0.92f, 0.08f);
    }

    @Override protected List<String> lines(boolean preview) {
        if (mc.level == null) return List.of();
        // 0 ticks = 06:00 in-game, one in-game hour = 1000 ticks.
        long t = Math.floorMod(mc.level.getDefaultClockTime() + 6000L, 24000L);
        int hh = (int) (t / 1000), mm = (int) ((t % 1000) * 60 / 1000);
        String time;
        if (hour24.on()) time = String.format("%02d:%02d", hh, mm);
        else time = String.format("%d:%02d %s", hh % 12 == 0 ? 12 : hh % 12, mm, hh < 12 ? "AM" : "PM");
        String p = "";
        if (phase.on()) {
            p = hh >= 7 && hh < 18 ? "  Day" : hh >= 18 && hh < 20 ? "  Sunset" : hh >= 5 && hh < 7 ? "  Sunrise" : "  Night";
        }
        return List.of("Time: " + time + p);
    }
}
