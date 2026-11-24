package com.voidnex.client.core;

import com.voidnex.client.setting.*;

import java.util.List;

/** Global client options (appearance, notifications, updates). Shown on the Appearance and Info pages. */
public final class ClientSettings {
    public static final BoolSetting dark = new BoolSetting("Dark Theme", "Switch between the dark and light interface.", true);
    public static final ModeSetting accent = new ModeSetting("Accent Color", "Neon accent used across the whole client.", "Purple", "Blue", "Red", "Green");
    public static final ModeSetting background = new ModeSetting("Menu Background", "Backdrop behind the menu hub. 'Custom Image' uses config/voidnex/background.png.", "Void", "Aurora", "Crimson", "Matrix", "Custom Image");
    public static final BoolSetting particles = new BoolSetting("Animated Particles", "Floating particles behind the menu.", true);
    public static final BoolSetting blur = new BoolSetting("Background Blur", "Blur the game world behind the menu.", true);
    public static final BoolSetting glow = new BoolSetting("Glow Effects", "Accent glow around panels and the backdrop.", true);
    public static final BoolSetting animations = new BoolSetting("Animations", "Smooth transitions in the interface.", true);
    public static final NumberSetting radius = new NumberSetting("Corner Radius", "Roundness of panels and buttons.", 5, 0, 10, 1);
    public static final NumberSetting opacity = new NumberSetting("Panel Opacity", "Opacity of the main menu panel.", 0.94, 0.5, 1.0, 0.02);
    public static final BoolSetting customLogo = new BoolSetting("Custom Logo", "Use config/voidnex/logo.png as the menu logo.", false);
    public static final BoolSetting notifications = new BoolSetting("Notifications", "Show toast notifications.", true);
    public static final BoolSetting notifyToggles = new BoolSetting("Module Toggle Alerts", "Notify when a module is enabled or disabled.", true);
    public static final NumberSetting notifySeconds = new NumberSetting("Notification Time", "Seconds a notification stays visible.", 3, 1, 8, 0.5);

    public static final StringSetting updateUrl = new StringSetting("Update URL", "URL of a JSON file: {\"version\":\"1.0.1\",\"url\":\"https://...\"}", "", 160);
    public static final BoolSetting checkUpdates = new BoolSetting("Check On Startup", "Check for updates when the game starts.", false);

    public static final List<Setting<?>> APPEARANCE = List.of(dark, accent, background, particles, blur, glow, animations,
            radius, opacity, customLogo, notifications, notifyToggles, notifySeconds);
    public static final List<Setting<?>> UPDATES = List.of(updateUrl, checkUpdates);

    private ClientSettings() {}
}
