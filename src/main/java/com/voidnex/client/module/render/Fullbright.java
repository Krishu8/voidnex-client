package com.voidnex.client.module.render;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Client-side brightness. Visual only; check your server's rules before using it. */
public class Fullbright extends ClientModule {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Night Vision: hidden client-side effect. Gamma: vanilla maximum brightness.", "Night Vision", "Gamma"));
    private String applied;
    private double savedGamma = -1;

    public Fullbright() {
        super("Fullbright", "See clearly in caves and at night.", Category.RENDER);
    }

    @Override public void onTick(Minecraft mc) {
        String m = mode.get();
        if (applied != null && !applied.equals(m)) cleanup(mc);
        if (m.equals("Gamma")) {
            if (applied == null) { savedGamma = mc.options.gamma().get(); applied = m; }
            mc.options.gamma().set(1.0);
        } else if (mc.player != null) {
            applied = m;
            if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
            }
        }
    }

    private void cleanup(Minecraft mc) {
        if ("Gamma".equals(applied) && savedGamma >= 0) mc.options.gamma().set(savedGamma);
        if ("Night Vision".equals(applied) && mc.player != null) {
            MobEffectInstance e = mc.player.getEffect(MobEffects.NIGHT_VISION);
            if (e != null && e.isInfiniteDuration()) mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
        applied = null;
        savedGamma = -1;
    }

    @Override protected void onDisable() { cleanup(mc); }
}
