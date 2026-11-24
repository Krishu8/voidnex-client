package com.voidnex.client.gui;

import com.voidnex.client.core.Config;
import com.voidnex.client.core.Theme;
import com.voidnex.client.module.HudModule;
import com.voidnex.client.module.ModuleManager;
import com.voidnex.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Drag-and-drop HUD editor.
 * Drag = move, scroll = scale, right-click = hide, chips at the bottom show / hide elements.
 */
public class HudEditorScreen extends Screen {
    private record Btn(int x, int y, int w, int h, String label, Runnable action) {}
    private record Chip(int x, int y, int w, int h, HudModule m) {}

    private final Screen parent;
    private final List<Btn> buttons = new ArrayList<>();
    private final List<Chip> chips = new ArrayList<>();
    private HudModule dragging;
    private int offX, offY;
    private boolean snap = true, guideV, guideH;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void extractBackground(GuiGraphicsExtractor c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0x66000000);
    }

    private HudModule hoveredHud(int mx, int my) {
        List<HudModule> l = ModuleManager.hudModules();
        for (int i = l.size() - 1; i >= 0; i--) {
            HudModule m = l.get(i);
            if (!m.isEnabled() || m.boundsW() == 0) continue;
            int x = m.boundsX(width), y = m.boundsY(height);
            if (mx >= x - 2 && mx <= x + m.boundsW() + 2 && my >= y - 2 && my <= y + m.boundsH() + 2) return m;
        }
        return null;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor c, int mx, int my, float delta) {
        extractBackground(c, mx, my, delta);
        int accent = Theme.accent();
        // center guides
        c.fill(width / 2, 0, width / 2 + 1, height, guideV ? accent : 0x22FFFFFF);
        c.fill(0, height / 2, width, height / 2 + 1, guideH ? accent : 0x22FFFFFF);

        HudModule hover = dragging != null ? dragging : hoveredHud(mx, my);
        for (HudModule m : ModuleManager.hudModules()) {
            if (!m.isEnabled()) continue;
            m.render(c, true);
            if (m.boundsW() == 0) continue;
            int x = m.boundsX(width), y = m.boundsY(height);
            boolean on = m == hover;
            RenderUtil.outline(c, x - 2, y - 2, m.boundsW() + 4, m.boundsH() + 4, 3, on ? accent : 0x66FFFFFF);
            if (on) {
                int ly = y > 12 ? y - 12 : y + m.boundsH() + 4;
                c.text(font, m.name + "  x" + String.format("%.2f", m.scale.d()), x, ly, accent, true);
            }
        }
        drawToolbar(c, mx, my);
    }

    private void drawToolbar(GuiGraphicsExtractor c, int mx, int my) {
        buttons.clear();
        chips.clear();
        List<HudModule> all = ModuleManager.hudModules();
        int maxW = Math.min(width - 24, 460);
        // layout chips with wrapping
        int x = 0, y = 0, rows = 1;
        List<int[]> pos = new ArrayList<>();
        for (HudModule m : all) {
            int cw = font.width(m.name) + 16;
            if (x + cw > maxW) { x = 0; y += 17; rows++; }
            pos.add(new int[]{x, y, cw});
            x += cw + 4;
        }
        int panelH = rows * 17 + 44, panelW = maxW + 16;
        int px = (width - panelW) / 2, py = height - panelH - 8;
        RenderUtil.glow(c, px, py, panelW, panelH, 6, Theme.accent() & 0xFFFFFF, 6, 0.5f);
        RenderUtil.rounded(c, px, py, panelW, panelH, 6, 0xF0100E1C);
        c.text(font, "HUD EDITOR", px + 8, py + 6, Theme.accent(), false);
        String hint = "Drag: move   Scroll: scale   Right-click: hide";
        c.text(font, hint, px + panelW - 8 - font.width(hint), py + 6, 0xFF8F8AB0, false);
        for (int i = 0; i < all.size(); i++) {
            HudModule m = all.get(i);
            int[] p = pos.get(i);
            int cx = px + 8 + p[0], cy = py + 20 + p[1];
            boolean hov = mx >= cx && mx < cx + p[2] && my >= cy && my < cy + 14;
            int bg = m.isEnabled() ? Theme.alpha(Theme.accent(), hov ? 0.55f : 0.4f) : (hov ? 0xFF2C2848 : 0xFF1C1A30);
            RenderUtil.rounded(c, cx, cy, p[2], 14, 4, bg);
            c.text(font, m.name, cx + 8, cy + 3, m.isEnabled() ? 0xFFFFFFFF : 0xFF8F8AB0, false);
            chips.add(new Chip(cx, cy, p[2], 14, m));
        }
        int by = py + panelH - 22;
        button(c, mx, my, px + 8, by, 80, 16, "Reset Layout", () -> {
            for (HudModule m : all) m.resetPosition();
        });
        button(c, mx, my, px + 94, by, 70, 16, "Snap: " + (snap ? "On" : "Off"), () -> snap = !snap);
        button(c, mx, my, px + panelW - 68, by, 60, 16, "Done", this::onClose);
    }

    private void button(GuiGraphicsExtractor c, int mx, int my, int x, int y, int w, int h, String label, Runnable a) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        RenderUtil.rounded(c, x, y, w, h, 4, hov ? 0xFF3A3560 : 0xFF2A2646);
        c.text(font, label, x + (w - font.width(label)) / 2, y + 4, 0xFFFFFFFF, false);
        buttons.add(new Btn(x, y, w, h, label, a));
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        for (Btn b : buttons) {
            if (mx >= b.x() && mx < b.x() + b.w() && my >= b.y() && my < b.y() + b.h()) { b.action().run(); return true; }
        }
        for (Chip ch : chips) {
            if (mx >= ch.x() && mx < ch.x() + ch.w() && my >= ch.y() && my < ch.y() + ch.h()) {
                ch.m().setEnabled(!ch.m().isEnabled(), false);
                return true;
            }
        }
        HudModule m = hoveredHud((int) mx, (int) my);
        if (m == null) return true;
        if (button == 1) { m.setEnabled(false, false); return true; }
        dragging = m;
        offX = (int) mx - m.boundsX(width);
        offY = (int) my - m.boundsY(height);
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x(), my = event.y();
        if (dragging == null) return false;
        int bw = dragging.boundsW(), bh = dragging.boundsH();
        int nx = (int) mx - offX, ny = (int) my - offY;
        guideV = guideH = false;
        if (snap) {
            int cxm = (width - bw) / 2, cym = (height - bh) / 2;
            if (Math.abs(nx - cxm) < 5) { nx = cxm; guideV = true; }
            if (Math.abs(ny - cym) < 5) { ny = cym; guideH = true; }
            if (nx < 6) nx = 0;
            if (ny < 6) ny = 0;
            if (nx + bw > width - 6) nx = width - bw;
            if (ny + bh > height - 6) ny = height - bh;
        }
        nx = dragging.clampX(width, nx);
        ny = dragging.clampY(height, ny);
        dragging.fx = nx / (float) width;
        dragging.fy = ny / (float) height;
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        guideV = guideH = false;
        Config.save();
        return true;
    }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        HudModule m = dragging != null ? dragging : hoveredHud((int) mx, (int) my);
        if (m != null) {
            m.scale.set(m.scale.d() + 0.05 * Math.signum(v));
            Config.save();
        }
        return true;
    }

    @Override public void onClose() {
        Config.save();
        minecraft.gui.setScreen(parent);
    }
}
