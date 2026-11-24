package com.voidnex.client.module.utility;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.ModeSetting;
import com.voidnex.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.server.level.ParticleStatus;

/** Applies performance-oriented video options while enabled and restores your own values afterwards. */
public class Performance extends ClientModule {
    private final NumberSetting view = add(new NumberSetting("Render Distance", "Chunks.", 10, 2, 32, 1));
    private final NumberSetting fps = add(new NumberSetting("Max FPS", "Frame limit (260 = unlimited).", 260, 30, 260, 10));
    private final ModeSetting particles = add(new ModeSetting("Particles", "Particle amount.", "All", "Decreased", "Minimal"));
    private final ModeSetting graphics = add(new ModeSetting("Graphics", "Graphics quality.", "Fast", "Fancy"));
    private int sView, sFps, counter;
    private ParticleStatus sPart;
    private GraphicsStatus sGfx;
    private boolean saved;

    public Performance() {
        super("Performance", "One-click FPS settings, restored when disabled.", Category.UTILITY);
    }

    @Override protected void onEnable() {
        Options o = mc.options;
        sView = o.renderDistance().get();
        sFps = o.framerateLimit().get();
        sPart = o.particles().get();
        sGfx = o.graphicsMode().get();
        saved = true;
        apply();
    }

    private void apply() {
        Options o = mc.options;
        o.renderDistance().set(view.i());
        o.framerateLimit().set(fps.i());
        o.particles().set(switch (particles.get()) {
            case "Decreased" -> ParticleStatus.DECREASED;
            case "Minimal" -> ParticleStatus.MINIMAL;
            default -> ParticleStatus.ALL;
        });
        o.graphicsMode().set(graphics.is("Fast") ? GraphicsStatus.FAST : GraphicsStatus.FANCY);
    }

    @Override public void onTick(Minecraft mc) {
        if (++counter >= 20) { counter = 0; apply(); }
    }

    @Override protected void onDisable() {
        if (!saved) return;
        Options o = mc.options;
        o.renderDistance().set(sView);
        o.framerateLimit().set(sFps);
        o.particles().set(sPart);
        o.graphicsMode().set(sGfx);
        saved = false;
    }
}
