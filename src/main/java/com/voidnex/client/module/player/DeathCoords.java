package com.voidnex.client.module.player;

import com.voidnex.client.core.Notifications;
import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.util.SessionStats;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class DeathCoords extends ClientModule {
    private final BoolSetting toast = add(new BoolSetting("Notification", "Show a toast with the coordinates.", true));
    private final BoolSetting chat = add(new BoolSetting("Chat Message", "Print the coordinates in your chat (only you see it).", true));
    private final BoolSetting clipboard = add(new BoolSetting("Copy To Clipboard", "Copy 'x y z' to the clipboard.", false));
    private int seen;

    public DeathCoords() {
        super("Death Coords", "Remembers where you died.", Category.PLAYER);
        defaultEnabled();
    }

    @Override protected void onEnable() { seen = SessionStats.deaths; }

    @Override public void onTick(Minecraft mc) {
        if (SessionStats.deaths == seen) return;
        seen = SessionStats.deaths;
        String xyz = String.format(Locale.ROOT, "%.0f %.0f %.0f", SessionStats.deathX, SessionStats.deathY, SessionStats.deathZ);
        String msg = xyz + " (" + SessionStats.deathDimension + ")";
        if (toast.on()) Notifications.push("You died", msg, Notifications.Type.ERROR);
        if (chat.on() && mc.player != null) mc.player.displayClientMessage(Component.literal("[VOIDNEX] Death at " + msg), false);
        if (clipboard.on()) mc.keyboardHandler.setClipboard(xyz);
    }
}
