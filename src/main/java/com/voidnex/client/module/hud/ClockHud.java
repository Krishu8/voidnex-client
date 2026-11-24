package com.voidnex.client.module.hud;

import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.BoolSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ClockHud extends TextHud {
    private final BoolSetting hour24 = add(new BoolSetting("24 Hour", "Use a 24-hour clock.", true));
    private final BoolSetting seconds = add(new BoolSetting("Show Seconds", "Include seconds.", false));

    public ClockHud() {
        super("Clock", "Shows your real-world time.", 0.92f, 0.02f);
    }

    @Override protected List<String> lines(boolean preview) {
        String pattern = (hour24.on() ? "HH:mm" : "hh:mm") + (seconds.on() ? ":ss" : "") + (hour24.on() ? "" : " a");
        return List.of(LocalTime.now().format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)));
    }
}
