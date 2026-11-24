package com.voidnex.client.module;

import com.voidnex.client.VoidnexClient;
import com.voidnex.client.core.Notifications;
import com.voidnex.client.util.Keys;
import com.voidnex.client.gui.HudEditorScreen;
import com.voidnex.client.module.combat.*;
import com.voidnex.client.module.hud.*;
import com.voidnex.client.module.misc.*;
import com.voidnex.client.module.movement.*;
import com.voidnex.client.module.player.*;
import com.voidnex.client.module.render.*;
import com.voidnex.client.module.utility.*;
import com.voidnex.client.module.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Owns every module, dispatches ticks / HUD rendering and polls module keybinds. */
public final class ModuleManager {
    private static final List<ClientModule> MODULES = new ArrayList<>();
    /** False until the game finished starting; before that, enabled modules are only flagged. */
    public static boolean started;

    private ModuleManager() {}

    public static void init() {
        // HUD
        add(new FpsHud()); add(new CpsHud()); add(new PingHud()); add(new CoordsHud());
        add(new KeystrokesHud()); add(new ArmorHud()); add(new PotionHud()); add(new TargetHud());
        add(new SpeedHud()); add(new ClockHud()); add(new SessionHud()); add(new CustomTextHud());
        // Combat
        add(new ComboCounter()); add(new ReachDisplay());
        // Movement
        add(new ToggleSprint()); add(new ToggleSneak()); add(new AutoJump());
        // Player
        add(new HealthAlert()); add(new ArmorAlert()); add(new DeathCoords());
        // Render
        add(new Fullbright()); add(new Zoom()); add(new CustomCrosshair());
        // World
        add(new WorldClock()); add(new ClearWeather());
        // Utility
        add(new AutoRespawn()); add(new Performance());
        // Misc
        add(new WindowTitle()); add(new MuteUnfocused());
    }

    private static void add(ClientModule m) { MODULES.add(m); }

    public static List<ClientModule> all() { return MODULES; }

    public static List<ClientModule> byCategory(Category c) {
        return MODULES.stream().filter(m -> m.category == c).toList();
    }

    public static List<HudModule> hudModules() {
        List<HudModule> l = new ArrayList<>();
        for (ClientModule m : MODULES) if (m instanceof HudModule h) l.add(h);
        return l;
    }

    public static long enabledCount() { return MODULES.stream().filter(ClientModule::isEnabled).count(); }

    public static <T extends ClientModule> T get(Class<T> type) {
        for (ClientModule m : MODULES) if (type.isInstance(m)) return type.cast(m);
        return null;
    }

    /** Called once the game is fully started: run onEnable for modules loaded as enabled. */
    public static void startup() {
        started = true;
        for (ClientModule m : MODULES) {
            if (!m.isEnabled()) continue;
            try { m.onEnable(); } catch (Throwable t) { VoidnexClient.LOG.error("Module {} failed to start", m.name, t); }
        }
    }

    public static void shutdown() {
        for (ClientModule m : MODULES) {
            try { m.onShutdown(); } catch (Throwable ignored) { }
        }
    }

    public static void tick(Minecraft mc) {
        if (mc.getWindow() != null) {
            long handle = mc.getWindow().handle();
            for (ClientModule m : MODULES) {
                if (m.key < 0) { m.keyWasDown = false; continue; }
                boolean down = mc.gui.screen() == null && Keys.isDown(handle, m.key);
                if (down == m.keyWasDown) continue;
                m.keyWasDown = down;
                if (m.hold()) m.setEnabled(down, false);
                else if (down) m.toggle();
            }
        }
        for (ClientModule m : MODULES) {
            if (!m.isEnabled()) continue;
            try {
                m.onTick(mc);
            } catch (Throwable t) {
                VoidnexClient.LOG.error("Module {} crashed and was disabled", m.name, t);
                m.setEnabled(false, false);
                Notifications.push(m.name, "Disabled after an error (see log)", Notifications.Type.ERROR);
            }
        }
    }

    public static void renderHud(GuiGraphicsExtractor ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        boolean editor = mc.gui.screen() instanceof HudEditorScreen;
        for (ClientModule m : MODULES) {
            if (!m.isEnabled() || (editor && m instanceof HudModule)) continue;
            m.onHudRender(ctx);
        }
    }
}
