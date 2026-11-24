package com.voidnex.client.util;

import net.minecraft.client.Minecraft;

/** Session-wide statistics shared by several modules. Ticked once per client tick. */
public final class SessionStats {
    public static final long START = System.currentTimeMillis();
    public static int deaths;
    public static double deathX, deathY, deathZ;
    public static String deathDimension = "";
    private static boolean wasDead;

    private SessionStats() {}

    public static void tick(Minecraft mc) {
        boolean dead = mc.player != null && mc.level != null && mc.player.getHealth() <= 0f;
        if (dead && !wasDead) {
            deaths++;
            deathX = mc.player.getX();
            deathY = mc.player.getY();
            deathZ = mc.player.getZ();
            deathDimension = mc.level.dimension().identifier().getPath();
        }
        wasDead = dead;
    }
}
