package com.voidnex.client.module.movement;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.util.Keys;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;

/** Press the sneak key once to sneak, press again to stop. */
public class ToggleSneak extends ClientModule {
    private final BoolSetting noFlying = add(new BoolSetting("Pause While Flying", "Do not force sneaking while flying (creative / elytra descent).", true));
    private boolean toggled, wasDown, forcing;

    public ToggleSneak() {
        super("Toggle Sneak", "Tap the sneak key to toggle sneaking.", Category.MOVEMENT);
    }

    @Override public void onTick(Minecraft mc) {
        if (mc.player == null) { toggled = false; return; }
        int code = KeyMappingHelper.getBoundKeyOf(mc.options.keyShift).getValue();
        boolean phys = mc.gui.screen() == null && Keys.isDown(mc.getWindow().handle(), code);
        if (phys && !wasDown) toggled = !toggled;
        wasDown = phys;
        boolean pause = noFlying.on() && mc.player.getAbilities().flying;
        boolean want = (toggled && !pause) || phys;
        if (want != forcing || want) mc.options.keyShift.setDown(want);
        forcing = want;
    }

    @Override protected void onDisable() {
        toggled = false;
        mc.options.keyShift.setDown(false);
    }
}
