package com.voidnex.client.module.hud;

import com.voidnex.client.core.Theme;
import com.voidnex.client.module.Category;
import com.voidnex.client.module.HudModule;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.ColorSetting;
import com.voidnex.client.util.Anim;
import com.voidnex.client.util.Cps;
import com.voidnex.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.KeyMapping;

import java.util.HashMap;
import java.util.Map;

public class KeystrokesHud extends HudModule {
    private final BoolSetting mouse = add(new BoolSetting("Show Mouse Buttons", "Show LMB and RMB.", true));
    private final BoolSetting cps = add(new BoolSetting("Show CPS On Buttons", "Show clicks per second under LMB / RMB.", true));
    private final BoolSetting space = add(new BoolSetting("Show Space Bar", "Show the jump key.", true));
    private final ColorSetting pressed = add(new ColorSetting("Pressed Color", "Key color while it is held.", 0xA855F7));
    private final Map<String, Anim> anims = new HashMap<>();

    public KeystrokesHud() {
        super("Keystrokes", "WASD, mouse buttons and space with press animations.", Category.HUD, 0.01f, 0.55f);
        defaultEnabled();
        background.set(false);
    }

    private void key(GuiGraphicsExtractor c, String label, int x, int y, int kw, int kh, boolean down, String sub) {
        Anim a = anims.computeIfAbsent(label, k -> new Anim(0));
        a.target = down ? 1 : 0;
        a.update(26);
        int bg = Theme.mix(0x99000000, 0xDD000000 | pressed.get(), a.value);
        RenderUtil.rounded(c, x, y, kw, kh, 3, bg);
        int fg = Theme.mix(0xFFFFFFFF, 0xFF0B0B12, a.value);
        int tw = mc.font.width(label);
        if (sub == null) {
            c.text(mc.font, label, x + (kw - tw) / 2, y + (kh - 8) / 2, fg, false);
        } else {
            c.text(mc.font, label, x + (kw - tw) / 2, y + 4, fg, false);
            c.text(mc.font, sub, x + (kw - mc.font.width(sub)) / 2, y + 14, Theme.alpha(fg, 0.8f), false);
        }
    }

    private static boolean down(KeyMapping k) { return k.isDown(); }

    @Override protected void draw(GuiGraphicsExtractor c, boolean preview) {
        int k = 20, g = 2, y = 0;
        w = 3 * k + 2 * g;
        key(c, "W", k + g, y, k, k, down(mc.options.keyUp), null);
        y += k + g;
        key(c, "A", 0, y, k, k, down(mc.options.keyLeft), null);
        key(c, "S", k + g, y, k, k, down(mc.options.keyDown), null);
        key(c, "D", 2 * (k + g), y, k, k, down(mc.options.keyRight), null);
        y += k;
        if (mouse.on()) {
            y += g;
            int bw = (w - g) / 2, bh = cps.on() ? 26 : k;
            key(c, "LMB", 0, y, bw, bh, down(mc.options.keyAttack), cps.on() ? Cps.left() + " CPS" : null);
            key(c, "RMB", bw + g, y, w - bw - g, bh, down(mc.options.keyUse), cps.on() ? Cps.right() + " CPS" : null);
            y += bh;
        }
        if (space.on()) {
            y += g;
            key(c, "-----", 0, y, w, 12, down(mc.options.keyJump), null);
            y += 12;
        }
        h = y;
    }
}
