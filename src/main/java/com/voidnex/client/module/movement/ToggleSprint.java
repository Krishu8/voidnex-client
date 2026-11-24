package com.voidnex.client.module.movement;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.BoolSetting;
import net.minecraft.client.Minecraft;

public class ToggleSprint extends ClientModule {
    private final BoolSetting forwardOnly = add(new BoolSetting("Forward Only", "Only sprint while holding the forward key.", true));
    private final BoolSetting notSneaking = add(new BoolSetting("Not While Sneaking", "Do not sprint while sneaking.", true));
    private boolean active;

    public ToggleSprint() {
        super("Toggle Sprint", "Sprints automatically, no key holding needed.", Category.MOVEMENT);
    }

    @Override public void onTick(Minecraft mc) {
        if (mc.player == null) return;
        boolean go = (mc.options.keyUp.isDown() || !forwardOnly.on()) && !(notSneaking.on() && mc.player.isShiftKeyDown());
        if (go) {
            mc.options.keySprint.setDown(true);
            active = true;
        } else if (active) {
            mc.options.keySprint.setDown(false);
            active = false;
        }
    }

    @Override protected void onDisable() {
        if (active) mc.options.keySprint.setDown(false);
        active = false;
    }
}
