package com.voidnex.client.module.render;

import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.module.ModuleManager;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.ColorSetting;
import com.voidnex.client.setting.ModeSetting;
import com.voidnex.client.setting.NumberSetting;
import com.voidnex.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.EntityHitResult;

/** Replaces the vanilla crosshair (cancelled by InGameHudMixin) with a customizable one. */
public class CustomCrosshair extends ClientModule {
    private final ModeSetting style = add(new ModeSetting("Style", "Crosshair shape.", "Cross", "Dot", "Circle", "Square"));
    private final NumberSetting size = add(new NumberSetting("Size", "Length / radius in pixels.", 5, 1, 15, 1));
    private final NumberSetting gap = add(new NumberSetting("Gap", "Gap in the center of the cross.", 2, 0, 10, 1));
    private final NumberSetting thick = add(new NumberSetting("Thickness", "Line thickness.", 1, 1, 4, 1));
    private final ColorSetting color = add(new ColorSetting("Color", "Crosshair color.", 0xFFFFFF));
    private final BoolSetting reactive = add(new BoolSetting("Red On Target", "Turn red while aiming at an entity.", true));
    private final BoolSetting outline = add(new BoolSetting("Outline", "Black outline for visibility (Cross and Dot).", true));

    public CustomCrosshair() {
        super("Custom Crosshair", "Design your own crosshair.", Category.RENDER);
        gap.showIf(() -> style.is("Cross"));
    }

    public static boolean replacesVanilla() {
        CustomCrosshair m = ModuleManager.get(CustomCrosshair.class);
        return m != null && m.isEnabled();
    }

    @Override public void onHudRender(GuiGraphicsExtractor c) {
        if (!mc.options.getCameraType().isFirstPerson()) return;
        int cx = c.guiWidth() / 2, cy = c.guiHeight() / 2;
        int col = 0xFF000000 | color.get();
        if (reactive.on() && mc.hitResult instanceof EntityHitResult) col = 0xFFFF4444;
        int s = size.i(), g = gap.i(), t = thick.i();
        switch (style.get()) {
            case "Circle" -> RenderUtil.outline(c, cx - s, cy - s, s * 2, s * 2, s, col);
            case "Square" -> RenderUtil.outline(c, cx - s, cy - s, s * 2, s * 2, 0, col);
            case "Dot" -> {
                if (outline.on()) c.fill(cx - t - 1, cy - t - 1, cx + t + 1, cy + t + 1, 0xFF000000);
                c.fill(cx - t, cy - t, cx + t, cy + t, col);
            }
            default -> {
                if (outline.on()) cross(c, cx, cy, s + 1, Math.max(0, g - 1), t + 2, 0xFF000000);
                cross(c, cx, cy, s, g, t, col);
            }
        }
    }

    private static void cross(GuiGraphicsExtractor c, int cx, int cy, int s, int g, int t, int col) {
        int a = -t / 2;
        c.fill(cx - g - s, cy + a, cx - g, cy + a + t, col);
        c.fill(cx + g, cy + a, cx + g + s, cy + a + t, col);
        c.fill(cx + a, cy - g - s, cx + a + t, cy - g, col);
        c.fill(cx + a, cy + g, cx + a + t, cy + g + s, col);
    }
}
