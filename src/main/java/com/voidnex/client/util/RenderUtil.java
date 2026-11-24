package com.voidnex.client.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small 2D drawing toolkit: rounded rectangles, outlines and glow built from GuiGraphicsExtractor fills. */
public final class RenderUtil {
    private RenderUtil() {}

    private static int inset(int r, int row) {
        double dy = r - row - 0.5;
        return (int) Math.round(r - Math.sqrt(r * r - dy * dy));
    }

    public static void rounded(GuiGraphicsExtractor c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            c.fill(x + in, y + i, x + w - in, y + i + 1, color);
            c.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, color);
        }
    }

    public static void outline(GuiGraphicsExtractor c, int x, int y, int w, int h, int r, int col) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            c.fill(x, y, x + w, y + 1, col);
            c.fill(x, y + h - 1, x + w, y + h, col);
            c.fill(x, y + 1, x + 1, y + h - 1, col);
            c.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
            return;
        }
        c.fill(x, y + r, x + 1, y + h - r, col);
        c.fill(x + w - 1, y + r, x + w, y + h - r, col);
        int prev = 0;
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            if (i == 0) {
                c.fill(x + in, y, x + w - in, y + 1, col);
                c.fill(x + in, y + h - 1, x + w - in, y + h, col);
            } else {
                int seg = Math.max(1, prev - in);
                c.fill(x + in, y + i, x + in + seg, y + i + 1, col);
                c.fill(x + w - in - seg, y + i, x + w - in, y + i + 1, col);
                c.fill(x + in, y + h - 1 - i, x + in + seg, y + h - i, col);
                c.fill(x + w - in - seg, y + h - 1 - i, x + w - in, y + h - i, col);
            }
            prev = in;
        }
    }

    /** Soft glow around a rounded rectangle (draw before the panel itself). */
    public static void glow(GuiGraphicsExtractor c, int x, int y, int w, int h, int r, int rgb, int spread, float strength) {
        for (int i = spread; i >= 1; i--) {
            float a = strength * (1f - (float) i / (spread + 1)) / 3f;
            int col = (Math.round(a * 255) << 24) | (rgb & 0xFFFFFF);
            rounded(c, x - i, y - i, w + 2 * i, h + 2 * i, r + i, col);
        }
    }

    /** Large soft radial blob made of concentric translucent discs (menu backdrop). */
    public static void blob(GuiGraphicsExtractor c, int cx, int cy, int radius, int rgb, float alpha) {
        for (int r = radius; r > 6; r -= 7) {
            int col = (Math.round(alpha * 255) << 24) | (rgb & 0xFFFFFF);
            rounded(c, cx - r, cy - r, r * 2, r * 2, r, col);
        }
    }
}
