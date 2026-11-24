package com.voidnex.client.module.world;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.setting.BoolSetting;
import net.minecraft.client.Minecraft;

/** Client-side only: hides rain and thunder visuals. */
public class ClearWeather extends ClientModule {
    private final BoolSetting rain = add(new BoolSetting("Hide Rain", "Remove rain and snow visuals.", true));
    private final BoolSetting thunder = add(new BoolSetting("Hide Thunder", "Remove the dark thunderstorm sky.", true));

    public ClearWeather() {
        super("Clear Weather", "Always sunny on your screen.", Category.WORLD);
    }

    @Override public void onTick(Minecraft mc) {
        if (mc.level == null) return;
        if (rain.on()) mc.level.setRainLevel(0f);
        if (thunder.on()) mc.level.setThunderLevel(0f);
    }
}
