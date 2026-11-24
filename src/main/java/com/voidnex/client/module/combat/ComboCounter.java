package com.voidnex.client.module.combat;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.NumberSetting;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionResult;

import java.util.List;

/** Counts your consecutive hits until you get hit or stop attacking. */
public class ComboCounter extends TextHud {
    private final NumberSetting resetDelay = add(new NumberSetting("Reset Delay", "Seconds without a hit before the combo resets.", 2.5, 1.0, 5.0, 0.5));
    private final BoolSetting hideZero = add(new BoolSetting("Hide At Zero", "Hide the element when there is no combo.", true));
    private int combo;
    private long lastHit;
    private boolean wasHurt;

    public ComboCounter() {
        super("Combo Counter", "Consecutive hits without taking damage.", Category.COMBAT, 0.45f, 0.45f);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide() && isEnabled() && entity instanceof LivingEntity le && le.hurtTime == 0) {
                combo++;
                lastHit = System.currentTimeMillis();
            }
            return InteractionResult.PASS;
        });
    }

    @Override public void onTick(Minecraft mc) {
        if (mc.player == null) { combo = 0; return; }
        boolean hurt = mc.player.hurtTime > 0;
        if (hurt && !wasHurt) combo = 0;
        wasHurt = hurt;
        if (combo > 0 && System.currentTimeMillis() - lastHit > resetDelay.d() * 1000) combo = 0;
    }

    @Override protected List<String> lines(boolean preview) {
        if (combo == 0 && hideZero.on()) return List.of();
        return List.of("Combo: " + combo);
    }
}
