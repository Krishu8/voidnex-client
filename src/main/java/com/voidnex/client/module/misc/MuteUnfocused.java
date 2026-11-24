package com.voidnex.client.module.misc;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

public class MuteUnfocused extends ClientModule {
    private final NumberSetting volume = add(new NumberSetting("Volume When Unfocused", "Fraction of your master volume while the window is in the background.", 0.0, 0.0, 1.0, 0.05));
    private boolean muted;
    private double saved;

    public MuteUnfocused() {
        super("Mute Unfocused", "Lowers volume while the game is in the background.", Category.MISC);
    }

    @Override public void onTick(Minecraft mc) {
        var opt = mc.options.getSoundSourceOptionInstance(SoundSource.MASTER);
        boolean focused = mc.isWindowActive();
        if (!focused && !muted) {
            saved = opt.get();
            muted = true;
        }
        if (!focused) opt.set(saved * volume.d());
        else if (muted) restore(mc);
    }

    private void restore(Minecraft mc) {
        mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(saved);
        muted = false;
    }

    @Override protected void onDisable() { if (muted) restore(mc); }
}
