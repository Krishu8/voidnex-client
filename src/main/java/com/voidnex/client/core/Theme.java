package com.voidnex.client.core;

/** Central color palette. Everything in the menu asks this class for its colors. */
public final class Theme {
    public static final int[] ACCENTS = {0xA855F7, 0x3B82F6, 0xEF4444, 0x22C55E};

    private Theme() {}

    private static boolean dark() { return ClientSettings.dark.on(); }

    public static int accent() { return 0xFF000000 | ACCENTS[ClientSettings.accent.index()]; }
    public static int accentSoft() { return alpha(accent(), 0.22f); }
    public static int panel() { return alpha(dark() ? 0xFF121020 : 0xFFFAFAFE, (float) ClientSettings.opacity.d()); }
    public static int card() { return dark() ? 0xFF1A1830 : 0xFFFFFFFF; }
    public static int cardHover() { return dark() ? 0xFF24213F : 0xFFEEECFB; }
    public static int track() { return dark() ? 0xFF2C2848 : 0xFFD9D6EC; }
    public static int text() { return dark() ? 0xFFF1EFFF : 0xFF17142A; }
    public static int sub() { return dark() ? 0xFF8F8AB0 : 0xFF6A6688; }
    public static int line() { return dark() ? 0xFF2A2646 : 0xFFDCD9EE; }
    public static int radius() { return ClientSettings.radius.i(); }

    public static int alpha(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 255) * a);
        return (al << 24) | (argb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = 0;
        for (int s = 24; s >= 0; s -= 8) {
            int ca = (a >>> s) & 255, cb = (b >>> s) & 255;
            r |= (Math.round(ca + (cb - ca) * t) & 255) << s;
        }
        return r;
    }
}
