package com.voidnex.client.core;

import com.voidnex.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Toast notification system. Slides in from the right, shows a progress bar, slides out. */
public final class Notifications {
    public enum Type { INFO, SUCCESS, WARN, ERROR }

    private static final class Toast {
        String title, msg;
        Type type;
        long born, life;
    }

    private static final List<Toast> TOASTS = new ArrayList<>();
    /** Set by the menu screen, which draws toasts itself on top of its own backdrop. */
    public static boolean suppressHud;

    private Notifications() {}

    public static void push(String title, String msg, Type type) {
        if (!ClientSettings.notifications.on()) return;
        Toast t = new Toast();
        t.title = title;
        t.msg = msg;
        t.type = type;
        t.born = System.currentTimeMillis();
        t.life = (long) (ClientSettings.notifySeconds.d() * 1000);
        TOASTS.add(t);
        while (TOASTS.size() > 5) TOASTS.remove(0);
    }

    private static int color(Type t) {
        return switch (t) {
            case SUCCESS -> 0xFF22C55E;
            case WARN -> 0xFFF59E0B;
            case ERROR -> 0xFFEF4444;
            default -> Theme.accent();
        };
    }

    public static void render(GuiGraphicsExtractor c) {
        long now = System.currentTimeMillis();
        TOASTS.removeIf(t -> now - t.born > t.life);
        if (TOASTS.isEmpty()) return;
        Font tr = Minecraft.getInstance().font;
        int sw = c.guiWidth();
        int y = 8;
        for (Toast t : TOASTS) {
            long age = now - t.born;
            float in = Math.min(1f, age / 250f), out = Math.min(1f, (t.life - age) / 250f);
            float k = Math.min(in, out);
            k = k * k * (3f - 2f * k);
            int w = Math.max(130, Math.max(tr.width(t.title), tr.width(t.msg)) + 24), h = 28;
            int x = sw - Math.round((w + 8) * k);
            int col = color(t.type);
            RenderUtil.rounded(c, x, y, w, h, 5, 0xEE0F0D1A);
            RenderUtil.rounded(c, x, y + 4, 3, h - 8, 1, col);
            c.text(tr, t.title, x + 10, y + 5, 0xFFFFFFFF, false);
            c.text(tr, t.msg, x + 10, y + 16, 0xFFA9A4C4, false);
            int bar = Math.round((w - 10) * (1f - (float) age / t.life));
            c.fill(x + 5, y + h - 3, x + 5 + Math.max(0, bar), y + h - 2, Theme.alpha(col, 0.8f));
            y += Math.round((h + 4) * k);
        }
    }
}
