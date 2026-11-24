package com.voidnex.client.module;

import com.google.gson.JsonObject;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.ColorSetting;
import com.voidnex.client.setting.NumberSetting;
import com.voidnex.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A module that draws an element on the HUD. Position is stored as a fraction of the screen
 * (so it adapts to any window size / GUI scale) and edited with the HUD editor.
 */
public abstract class HudModule extends ClientModule {
    public float fx, fy;
    private final float defX, defY;

    public final NumberSetting scale = add(new NumberSetting("Scale", "Size of this HUD element.", 1.0, 0.5, 2.0, 0.05));
    public final BoolSetting background = add(new BoolSetting("Background", "Draw a translucent panel behind the element.", true));
    public final NumberSetting bgOpacity = add(new NumberSetting("Background Opacity", "Opacity of the panel.", 0.45, 0.0, 1.0, 0.05));
    public final ColorSetting color = add(new ColorSetting("Text Color", "Color of the text.", 0xFFFFFF));
    public final BoolSetting shadow = add(new BoolSetting("Text Shadow", "Draw a shadow under the text.", true));

    /** Unscaled size from the last draw call. */
    protected int w, h;

    protected HudModule(String name, String description, Category category, float fx, float fy) {
        super(name, description, category);
        this.fx = defX = fx;
        this.fy = defY = fy;
        bgOpacity.showIf(background::on);
    }

    /** Draw at origin (0,0). Must update {@link #w} and {@link #h}. {@code preview} = HUD editor. */
    protected abstract void draw(GuiGraphicsExtractor c, boolean preview);

    public void resetPosition() {
        fx = defX;
        fy = defY;
        scale.set(1.0);
    }

    // ---- geometry -----------------------------------------------------------------------------
    public int boundsW() { return Math.max(0, Math.round(w * scale.f())); }
    public int boundsH() { return Math.max(0, Math.round(h * scale.f())); }
    public int clampX(int sw, int x) { return Math.max(0, Math.min(x, Math.max(0, sw - boundsW()))); }
    public int clampY(int sh, int y) { return Math.max(0, Math.min(y, Math.max(0, sh - boundsH()))); }
    public int boundsX(int sw) { return clampX(sw, Math.round(fx * sw)); }
    public int boundsY(int sh) { return clampY(sh, Math.round(fy * sh)); }

    public final void render(GuiGraphicsExtractor c, boolean preview) {
        var pose = c.pose();
        pose.pushMatrix();
        pose.translate((float) boundsX(c.guiWidth()), boundsY(c.guiHeight()));
        float s = scale.f();
        pose.scale(s, s);
        draw(c, preview);
        pose.popMatrix();
    }

    @Override public void onHudRender(GuiGraphicsExtractor ctx) { render(ctx, false); }

    // ---- helpers for subclasses ---------------------------------------------------------------
    protected void box(GuiGraphicsExtractor c, int bw, int bh) {
        if (background.on()) RenderUtil.rounded(c, 0, 0, bw, bh, 3, ((int) (bgOpacity.f() * 255) << 24) | 0x0B0B12);
    }

    protected void text(GuiGraphicsExtractor c, String s, int x, int y, int rgb) {
        c.text(mc.font, s, x, y, 0xFF000000 | rgb, shadow.on());
    }

    // ---- persistence --------------------------------------------------------------------------
    @Override public JsonObject saveState() {
        JsonObject o = super.saveState();
        o.addProperty("x", fx);
        o.addProperty("y", fy);
        return o;
    }

    @Override public void loadState(JsonObject o) {
        super.loadState(o);
        try {
            if (o.has("x")) fx = o.get("x").getAsFloat();
            if (o.has("y")) fy = o.get("y").getAsFloat();
        } catch (Exception ignored) { }
    }
}
