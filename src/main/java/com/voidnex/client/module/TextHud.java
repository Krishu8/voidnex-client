package com.voidnex.client.module;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** A HUD element that is just a few lines of text. Subclasses only implement {@link #lines}. */
public abstract class TextHud extends HudModule {
    protected TextHud(String name, String description, float fx, float fy) {
        this(name, description, Category.HUD, fx, fy);
    }

    protected TextHud(String name, String description, Category category, float fx, float fy) {
        super(name, description, category, fx, fy);
    }

    /** Lines to show. Return an empty list to hide the element. */
    protected abstract List<String> lines(boolean preview);

    protected int colorFor(int line) { return color.get(); }

    @Override protected void draw(GuiGraphicsExtractor c, boolean preview) {
        List<String> ls = mc.player == null ? List.of() : lines(preview);
        if (ls.isEmpty()) {
            if (!preview) { w = 0; h = 0; return; }
            ls = List.of(name);
        }
        int tw = 0;
        for (String s : ls) tw = Math.max(tw, mc.font.width(s));
        w = tw + 10;
        h = ls.size() * 11 + 3;
        box(c, w, h);
        for (int i = 0; i < ls.size(); i++) text(c, ls.get(i), 5, 3 + i * 11, colorFor(i));
    }
}
