package com.voidnex.client.module.hud;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.HudModule;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.NumberSetting;
import com.voidnex.client.util.Anim;
import com.voidnex.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.util.Mth;

import java.util.Locale;

public class TargetHud extends HudModule {
    private final NumberSetting hold = add(new NumberSetting("Hold Time", "Seconds to keep showing the target after you look away.", 3.0, 0.5, 10.0, 0.5));
    private final BoolSetting distance = add(new BoolSetting("Show Distance", "Show the distance to the target.", true));
    private final BoolSetting animate = add(new BoolSetting("Animate Health Bar", "Smoothly animate health changes.", true));
    private LivingEntity target;
    private long lastSeen;
    private final Anim bar = new Anim(1);

    public TargetHud() {
        super("Target Info", "Name and health of the entity you are looking at.", Category.HUD, 0.38f, 0.66f);
    }

    @Override public void onTick(Minecraft mc) {
        if (mc.player == null) { target = null; return; }
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity le && le.isAlive()) {
            if (le != target) bar.value = le.getHealth() / Math.max(1f, le.getMaxHealth());
            target = le;
            lastSeen = System.currentTimeMillis();
        } else if (target != null && (!target.isAlive() || System.currentTimeMillis() - lastSeen > hold.d() * 1000)) {
            target = null;
        }
    }

    @Override protected void draw(GuiGraphicsExtractor c, boolean preview) {
        LivingEntity t = target;
        if (t == null) {
            if (!preview) { w = h = 0; return; }
            t = mc.player;
        }
        String name = t.getDisplayName().getString();
        float hp = t.getHealth(), max = Math.max(1f, t.getMaxHealth());
        String info = String.format(Locale.ROOT, "%.1f / %.1f", hp, max)
                + (distance.on() && t != mc.player ? String.format(Locale.ROOT, "  %.1fm", mc.player.distanceTo(t)) : "");
        w = Math.max(110, Math.max(mc.font.width(name), mc.font.width(info)) + 12);
        h = 33;
        box(c, w, h);
        text(c, name, 6, 4, color.get());
        float pct = Mth.clamp(hp / max, 0f, 1f);
        bar.target = pct;
        if (animate.on()) bar.update(10); else bar.value = pct;
        RenderUtil.rounded(c, 6, 16, w - 12, 5, 2, 0x66000000);
        int fill = Math.max(2, Math.round((w - 12) * bar.value));
        RenderUtil.rounded(c, 6, 16, fill, 5, 2, 0xFF000000 | Mth.hsvToRgb(pct / 3f, 0.75f, 1f));
        text(c, info, 6, 23, 0xBBBBBB);
    }
}
