package com.voidnex.client.module.utility;

import com.voidnex.client.core.Notifications;
import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

public class AutoRespawn extends ClientModule {
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks to wait on the death screen (20 = 1 second).", 20, 0, 100, 5));
    private final BoolSetting notify = add(new BoolSetting("Notify", "Show a toast when respawning.", false));
    private int wait;

    public AutoRespawn() {
        super("Auto Respawn", "Respawns for you after you die.", Category.UTILITY);
    }

    @Override public void onTick(Minecraft mc) {
        if (mc.player != null && mc.level != null && mc.gui.screen() instanceof DeathScreen && !mc.level.getLevelData().isHardcore()) {
            if (++wait >= delay.i()) {
                mc.player.respawn();
                if (notify.on()) Notifications.push("Auto Respawn", "Respawning...", Notifications.Type.INFO);
                wait = 0;
            }
        } else {
            wait = 0;
        }
    }
}
