package com.voidnex.client.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/** Clicks-per-second tracker. Polls the mouse buttons every frame (no mixin needed). */
public final class Cps {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();
    private static boolean wasLeft, wasRight;

    private Cps() {}

    /** Call once per rendered frame. Only counts clicks made while no screen is open. */
    public static void poll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;
        long h = mc.getWindow().handle();
        boolean open = mc.gui.screen() != null;
        boolean l = !open && GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = !open && GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (l && !wasLeft) LEFT.addLast(now);
        if (r && !wasRight) RIGHT.addLast(now);
        wasLeft = l;
        wasRight = r;
    }

    public static int left() { return count(LEFT); }
    public static int right() { return count(RIGHT); }

    private static int count(Deque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }
}
