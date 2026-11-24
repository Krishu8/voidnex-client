package com.voidnex.client.module.hud;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.HudModule;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.ModeSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ArmorHud extends HudModule {
    private final ModeSetting layout = add(new ModeSetting("Layout", "Stack the pieces vertically or in a row.", "Vertical", "Horizontal"));
    private final ModeSetting durability = add(new ModeSetting("Durability", "How durability is displayed.", "Percent", "Value", "None"));
    private final BoolSetting held = add(new BoolSetting("Show Held Item", "Also show the item in your main hand.", false));
    private final BoolSetting empty = add(new BoolSetting("Show Empty Slots", "Keep empty armor slots in the layout.", false));

    public ArmorHud() {
        super("Armor Status", "Shows your armor pieces and their durability.", Category.HUD, 0.90f, 0.45f);
        defaultEnabled();
        background.set(false);
    }

    private String durText(ItemStack s) {
        if (!s.isDamageableItem()) return "";
        int left = s.getMaxDamage() - s.getDamageValue();
        return durability.is("Percent") ? Math.round(100f * left / s.getMaxDamage()) + "%" : String.valueOf(left);
    }

    private int durColor(ItemStack s) {
        if (!s.isDamageableItem()) return 0xFFFFFF;
        float pct = (float) (s.getMaxDamage() - s.getDamageValue()) / s.getMaxDamage();
        return Mth.hsvToRgb(pct / 3f, 0.8f, 1f);
    }

    @Override protected void draw(GuiGraphicsExtractor c, boolean preview) {
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = mc.player.getItemBySlot(slot);
            if (!st.isEmpty() || empty.on()) items.add(st);
        }
        if (held.on() && !mc.player.getMainHandItem().isEmpty()) items.add(mc.player.getMainHandItem());

        if (items.isEmpty()) {
            if (!preview) { w = h = 0; return; }
            w = 70; h = 14;
            box(c, w, h);
            text(c, "Armor Status", 5, 3, color.get());
            return;
        }

        boolean showText = !durability.is("None");
        int n = items.size();
        if (layout.is("Vertical")) {
            w = 8 + 16 + (showText ? 34 : 0);
            h = n * 18 + 4;
            box(c, w, h);
            for (int i = 0; i < n; i++) {
                ItemStack st = items.get(i);
                int y = 2 + i * 18;
                if (!st.isEmpty()) {
                    c.item(st, 4, y);
                    if (showText) text(c, durText(st), 24, y + 4, durColor(st));
                }
            }
        } else {
            int cell = showText ? 32 : 20;
            w = n * cell + 4;
            h = 4 + 16 + (showText ? 11 : 0);
            box(c, w, h);
            for (int i = 0; i < n; i++) {
                ItemStack st = items.get(i);
                int x = 2 + i * cell + (cell - 16) / 2;
                if (!st.isEmpty()) {
                    c.item(st, x, 2);
                    if (showText) {
                        String t = durText(st);
                        text(c, t, 2 + i * cell + (cell - mc.font.width(t)) / 2, 20, durColor(st));
                    }
                }
            }
        }
    }
}
