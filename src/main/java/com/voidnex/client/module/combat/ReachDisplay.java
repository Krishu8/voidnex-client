package com.voidnex.client.module.combat;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.NumberSetting;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/** Measures the distance of your own last hit (eye to closest point of the target's hitbox). */
public class ReachDisplay extends TextHud {
    private final NumberSetting decimals = add(new NumberSetting("Decimals", "Digits after the decimal point.", 2, 1, 3, 1));
    private final NumberSetting showTime = add(new NumberSetting("Display Time", "Seconds the last value stays visible.", 5.0, 1.0, 20.0, 1.0));
    private double reach = -1;
    private long when;

    public ReachDisplay() {
        super("Reach Display", "Distance of your last hit.", Category.COMBAT, 0.45f, 0.50f);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide() && isEnabled()) {
                AABB b = entity.getBoundingBox();
                Vec3 eye = player.getEyePosition();
                Vec3 p = new Vec3(Mth.clamp(eye.x, b.minX, b.maxX), Mth.clamp(eye.y, b.minY, b.maxY), Mth.clamp(eye.z, b.minZ, b.maxZ));
                reach = eye.distanceTo(p);
                when = System.currentTimeMillis();
            }
            return InteractionResult.PASS;
        });
    }

    @Override protected List<String> lines(boolean preview) {
        if (reach < 0 || System.currentTimeMillis() - when > showTime.d() * 1000) return List.of();
        return List.of(String.format(Locale.ROOT, "Reach: %." + decimals.i() + "f", reach));
    }
}
