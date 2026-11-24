package com.voidnex.client.module.misc;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.StringSetting;
import net.minecraft.client.Minecraft;

public class WindowTitle extends ClientModule {
    private final StringSetting title = add(new StringSetting("Title", "Placeholders: {version} {user}", "VOIDNEX CLIENT - {version}", 64));
    private int counter;

    public WindowTitle() {
        super("Window Title", "Custom game window title.", Category.MISC);
    }

    private void apply(Minecraft mc) {
        mc.getWindow().setTitle(title.get().replace("{version}", mc.getLaunchedVersion()).replace("{user}", mc.getUser().getName()));
    }

    @Override protected void onEnable() { apply(mc); }

    @Override public void onTick(Minecraft mc) {
        if (++counter >= 20) { counter = 0; apply(mc); }
    }

    @Override protected void onDisable() { mc.updateTitle(); }
}
