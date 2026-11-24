package com.voidnex.client.module.player;

import com.voidnex.client.core.Notifications;
import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.NumberSetting;
import com.voidnex.client.util.Info;
import net.minecraft.client.Minecraft;

import java.util.Locale;

public class HealthAlert extends ClientModule {
    private final NumberSetting threshold = add(new NumberSetting("Threshold", "Warn at or below this many hearts.", 4, 1, 10, 0.5));
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Seconds between warnings.", 8, 3, 30, 1));
    private final BoolSetting sound = add(new BoolSetting("Sound", "Play a warning sound.", true));
    private long last;

    public HealthAlert() {
        super("Health Alert", "Warns you when your health gets low.", Category.PLAYER);
    }

    @Override public void onTick(Minecraft mc) {
        if (mc.player == null || !mc.player.isAlive()) return;
        float hearts = mc.player.getHealth() / 2f;
        long now = System.currentTimeMillis();
        if (hearts <= threshold.d() && now - last > cooldown.d() * 1000) {
            Notifications.push("Low Health", String.format(Locale.ROOT, "%.1f hearts remaining", hearts), Notifications.Type.WARN);
            if (sound.on()) Info.beep(0.6f);
            last = now;
        }
    }
}
