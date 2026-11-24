package com.voidnex.client.module.player;

import com.voidnex.client.core.Notifications;
import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.NumberSetting;
import com.voidnex.client.util.Info;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ArmorAlert extends ClientModule {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final NumberSetting percent = add(new NumberSetting("Durability Threshold", "Warn when a piece drops below this percent.", 15, 5, 50, 1));
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Seconds between warnings per piece.", 30, 5, 120, 5));
    private final BoolSetting sound = add(new BoolSetting("Sound", "Play a warning sound.", true));
    private final long[] last = new long[4];

    public ArmorAlert() {
        super("Armor Alert", "Warns you when armor is about to break.", Category.PLAYER);
    }

    @Override public void onTick(Minecraft mc) {
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack st = mc.player.getItemBySlot(SLOTS[i]);
            if (st.isEmpty() || !st.isDamageableItem() || st.getMaxDamage() <= 0) continue;
            float pct = 100f * (st.getMaxDamage() - st.getDamageValue()) / st.getMaxDamage();
            if (pct < percent.d() && now - last[i] > cooldown.d() * 1000) {
                Notifications.push("Armor Low", st.getHoverName().getString() + " at " + Math.round(pct) + "%", Notifications.Type.WARN);
                if (sound.on()) Info.beep(0.8f);
                last[i] = now;
            }
        }
    }
}
