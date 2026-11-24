package com.voidnex.client.module.movement;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.BoolSetting;

/** Turns on vanilla Auto-Jump (jump up single blocks automatically) while enabled. */
public class AutoJump extends ClientModule {
    private final BoolSetting restore = add(new BoolSetting("Restore On Disable", "Put your previous Auto-Jump option back when disabled.", true));
    private boolean previous;

    public AutoJump() {
        super("Auto Jump", "Automatically hop up single-block steps.", Category.MOVEMENT);
    }

    @Override protected void onEnable() {
        previous = mc.options.autoJump().get();
        mc.options.autoJump().set(true);
    }

    @Override protected void onDisable() {
        mc.options.autoJump().set(restore.on() ? previous : false);
    }
}
