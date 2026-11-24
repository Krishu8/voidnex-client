package com.voidnex.client.module.render;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Zooms by lowering the FOV option (vanilla minimum is 30) and optionally lowering mouse sensitivity. */
public class Zoom extends ClientModule {
    private final NumberSetting fov = add(new NumberSetting("Zoom FOV", "FOV while zoomed. Lower = more zoom (minimum 30).", 30, 30, 70, 1));
    private final BoolSetting holdMode = add(new BoolSetting("Hold To Zoom", "Zoom only while the key is held.", true));
    private final BoolSetting slow = add(new BoolSetting("Reduce Sensitivity", "Lower mouse sensitivity while zoomed.", true));
    private final NumberSetting sens = add(new NumberSetting("Sensitivity Multiplier", "Mouse sensitivity multiplier while zoomed.", 0.5, 0.1, 1.0, 0.05));
    private int savedFov;
    private double savedSens;
    private boolean zoomed;

    public Zoom() {
        super("Zoom", "Hold a key to zoom in.", Category.RENDER);
        key = GLFW.GLFW_KEY_C;
        sens.showIf(slow::on);
    }

    @Override public boolean hold() { return holdMode.on(); }

    @Override protected void onEnable() {
        savedFov = mc.options.fov().get();
        savedSens = mc.options.sensitivity().get();
        zoomed = true;
        apply();
    }

    private void apply() {
        if (mc.options.fov().get() != fov.i()) mc.options.fov().set(fov.i());
        if (slow.on()) mc.options.sensitivity().set(Math.min(1.0, savedSens * sens.d()));
        else mc.options.sensitivity().set(savedSens);
    }

    @Override public void onTick(Minecraft mc) { if (zoomed) apply(); }

    @Override protected void onDisable() {
        if (!zoomed) return;
        mc.options.fov().set(savedFov);
        mc.options.sensitivity().set(savedSens);
        zoomed = false;
    }
}
