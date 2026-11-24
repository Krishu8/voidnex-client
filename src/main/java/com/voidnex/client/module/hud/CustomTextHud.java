package com.voidnex.client.module.hud;

import com.voidnex.client.core.Theme;
import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.StringSetting;
import com.voidnex.client.util.Cps;
import com.voidnex.client.util.Info;
import net.minecraft.util.Mth;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CustomTextHud extends TextHud {
    private final StringSetting text = add(new StringSetting("Text",
            "Placeholders: {name} {fps} {ping} {cps} {x} {y} {z} {time}", "{name} | {fps} FPS | {ping} ms", 64));
    private final BoolSetting accent = add(new BoolSetting("Use Accent Color", "Use the client accent color instead of Text Color.", false));
    private final BoolSetting chroma = add(new BoolSetting("Chroma", "Animated rainbow text.", false));

    public CustomTextHud() {
        super("Custom Text", "Your own text with live placeholders.", 0.40f, 0.02f);
    }

    @Override protected List<String> lines(boolean preview) {
        int ping = Info.ping();
        String s = text.get()
                .replace("{name}", mc.getUser().getName())
                .replace("{fps}", String.valueOf(mc.getFps()))
                .replace("{ping}", ping < 0 ? "--" : String.valueOf(ping))
                .replace("{cps}", String.valueOf(Cps.left()))
                .replace("{x}", String.format(Locale.ROOT, "%.0f", mc.player.getX()))
                .replace("{y}", String.format(Locale.ROOT, "%.0f", mc.player.getY()))
                .replace("{z}", String.format(Locale.ROOT, "%.0f", mc.player.getZ()))
                .replace("{time}", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        return s.isBlank() ? List.of() : List.of(s);
    }

    @Override protected int colorFor(int line) {
        if (chroma.on()) return Mth.hsvToRgb((System.currentTimeMillis() % 4000) / 4000f, 0.7f, 1f);
        if (accent.on()) return Theme.accent() & 0xFFFFFF;
        return super.colorFor(line);
    }
}
