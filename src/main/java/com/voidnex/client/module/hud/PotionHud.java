package com.voidnex.client.module.hud;

import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.ModeSetting;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

import static com.voidnex.client.util.Info.duration;
import static com.voidnex.client.util.Info.roman;

public class PotionHud extends TextHud {
    private final BoolSetting time = add(new BoolSetting("Show Duration", "Show the remaining time.", true));
    private final ModeSetting colors = add(new ModeSetting("Color Mode", "Color effects by good / bad, or use the text color.", "Good / Bad", "Text Color"));
    private final List<Integer> lineColors = new ArrayList<>();

    public PotionHud() {
        super("Potion Effects", "Lists your active status effects.", 0.86f, 0.10f);
        defaultEnabled();
    }

    @Override protected List<String> lines(boolean preview) {
        lineColors.clear();
        List<String> out = new ArrayList<>();
        for (MobEffectInstance e : mc.player.getActiveEffects()) {
            if (!e.showIcon()) continue;
            String name = e.getEffect().value().getDisplayName().getString();
            int amp = e.getAmplifier() + 1;
            long ms = e.getDuration() * 50L;
            String t = e.isInfiniteDuration() ? "inf" : ms >= 3_600_000L ? duration(ms) : duration(ms).substring(3);
            out.add(name + (amp > 1 ? " " + roman(amp) : "") + (time.on() ? "  " + t : ""));
            lineColors.add(e.getEffect().value().isBeneficial() ? 0x7CF29A : 0xFF7C7C);
        }
        if (out.isEmpty() && preview) {
            out.add("Speed II  01:30");
            lineColors.add(0x7CF29A);
        }
        return out;
    }

    @Override protected int colorFor(int line) {
        if (colors.is("Good / Bad") && line < lineColors.size()) return lineColors.get(line);
        return super.colorFor(line);
    }
}
