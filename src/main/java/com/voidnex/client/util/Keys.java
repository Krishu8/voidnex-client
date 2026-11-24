package com.voidnex.client.util;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.Locale;

public final class Keys {
    private Keys() {}

    public static String name(int key) {
        if (key < 0) return "NONE";
        try {
            return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "KEY " + key;
        }
    }

    /** True while the given GLFW key is physically held. */
    public static boolean isDown(long window, int key) {
        return key >= 0 && org.lwjgl.glfw.GLFW.glfwGetKey(window, key) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }
}
