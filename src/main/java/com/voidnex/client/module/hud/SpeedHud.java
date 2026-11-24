package com.voidnex.client.module.hud;

import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public class SpeedHud extends TextHud {
    private final ModeSetting unit = add(new ModeSetting("Unit", "Unit of the speed value.", "Blocks/s", "km/h"));
    private final BoolSetting vertical = add(new BoolSetting("Include Vertical", "Count vertical movement too (falling, flying).", false));
    private Vec3 last;
    private double speed;

    public SpeedHud() {
        super("Speed", "Shows how fast you are moving.", 0.01f, 0.30f);
    }

    @Override public void onTick(Minecraft mc) {
        if (mc.player == null) { last = null; return; }
        Vec3 pos = mc.player.position();
        if (last != null) {
            double dx = pos.x - last.x, dy = pos.y - last.y, dz = pos.z - last.z;
            double d = Math.sqrt(dx * dx + dz * dz + (vertical.on() ? dy * dy : 0)) * 20.0;
            speed = speed * 0.7 + d * 0.3;
        }
        last = pos;
    }

    @Override protected List<String> lines(boolean preview) {
        boolean kmh = unit.is("km/h");
        return List.of(String.format(Locale.ROOT, "Speed: %.1f %s", kmh ? speed * 3.6 : speed, kmh ? "km/h" : "b/s"));
    }
}
