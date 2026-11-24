package com.voidnex.client;

import com.voidnex.client.core.ClientSettings;
import com.voidnex.client.core.Config;
import com.voidnex.client.core.Notifications;
import com.voidnex.client.core.UpdateChecker;
import com.voidnex.client.gui.MenuScreen;
import com.voidnex.client.module.ModuleManager;
import com.voidnex.client.module.render.CustomCrosshair;
import com.voidnex.client.util.Cps;
import com.voidnex.client.util.Images;
import com.voidnex.client.util.SessionStats;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoidnexClient implements ClientModInitializer {
    public static final String ID = "voidnex";
    public static final Logger LOG = LoggerFactory.getLogger("VOIDNEX");
    public static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main"));
        menuKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.voidnex.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            SessionStats.tick(mc);
            while (menuKey.consumeClick()) {
                if (mc.gui.screen() == null) mc.gui.setScreen(new MenuScreen(null));
            }
            ModuleManager.tick(mc);
        });

        // All module HUD elements and notifications are drawn in one HUD layer (on top of the vanilla HUD).
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(ID, "hud"), (graphics, tick) -> {
            Cps.poll();
            ModuleManager.renderHud(graphics);
            if (!Notifications.suppressHud) Notifications.render(graphics);
        });

        // The Custom Crosshair module replaces the vanilla crosshair element while it is enabled.
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, tick) -> {
            if (!CustomCrosshair.replacesVanilla()) original.extractRenderState(graphics, tick);
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
            ModuleManager.startup();
            Images.reload();
            if (ClientSettings.checkUpdates.on()) UpdateChecker.check(false);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            Config.save();
            ModuleManager.shutdown();
        });

        // "VOIDNEX" button on the title screen and the pause menu.
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                Screens.getWidgets(screen).add(Button.builder(Component.literal("VOIDNEX"),
                        b -> client.gui.setScreen(new MenuScreen(screen))).bounds(6, 6, 64, 20).build());
            }
        });

        LOG.info("VOIDNEX CLIENT loaded with {} modules", ModuleManager.all().size());
    }
}
