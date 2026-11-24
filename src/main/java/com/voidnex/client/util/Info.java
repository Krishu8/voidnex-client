package com.voidnex.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class Info {
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private Info() {}

    /** Latency in ms, or -1 when not connected. */
    public static int ping() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return -1;
        PlayerInfo e = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return e == null ? -1 : e.getLatency();
    }

    public static String server() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip;
        return mc.isSingleplayer() ? "Singleplayer" : "Not connected";
    }

    public static String duration(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format("%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    public static String roman(int n) { return n >= 1 && n <= 10 ? ROMAN[n] : String.valueOf(n); }

    public static void beep(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }
}
