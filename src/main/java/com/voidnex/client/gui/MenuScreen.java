package com.voidnex.client.gui;

import com.voidnex.client.VoidnexClient;
import com.voidnex.client.core.ClientSettings;
import com.voidnex.client.core.Config;
import com.voidnex.client.core.Notifications;
import com.voidnex.client.core.Theme;
import com.voidnex.client.core.UpdateChecker;
import com.voidnex.client.module.Category;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.module.HudModule;
import com.voidnex.client.module.ModuleManager;
import com.voidnex.client.setting.*;
import com.voidnex.client.util.Anim;
import com.voidnex.client.util.Cps;
import com.voidnex.client.util.Images;
import com.voidnex.client.util.Info;
import com.voidnex.client.util.Keys;
import com.voidnex.client.util.RenderUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * The VOIDNEX hub: sidebar navigation, module cards with live settings, appearance, configs,
 * profiles, keybind manager and info page. Immediate-mode: every frame the layout is drawn and
 * click regions are re-registered, so the UI is always in sync with the data.
 */
public class MenuScreen extends Screen {
    private interface Click { void run(double mx, int button); }
    private interface Drag { void run(double mx); }
    private record Hit(int x, int y, int w, int h, Click action) {}
    private record Nav(String label, Page page, int kind) {} // kind: 0 page, 1 header, 2 HUD editor

    private enum Page {
        HOME(null), COMBAT(Category.COMBAT), MOVEMENT(Category.MOVEMENT), PLAYER(Category.PLAYER),
        RENDER(Category.RENDER), WORLD(Category.WORLD), UTILITY(Category.UTILITY), HUD(Category.HUD),
        MISC(Category.MISC), FAVORITES(null), APPEARANCE(null), CONFIGS(null), PROFILES(null),
        KEYBINDS(null), INFO(null);
        final Category cat;
        Page(Category cat) { this.cat = cat; }
    }

    private static final List<Nav> NAV = List.of(
            new Nav("Home", Page.HOME, 0), new Nav("MODULES", null, 1),
            new Nav("Combat", Page.COMBAT, 0), new Nav("Movement", Page.MOVEMENT, 0), new Nav("Player", Page.PLAYER, 0),
            new Nav("Render", Page.RENDER, 0), new Nav("World", Page.WORLD, 0), new Nav("Utility", Page.UTILITY, 0),
            new Nav("HUD", Page.HUD, 0), new Nav("Misc", Page.MISC, 0), new Nav("Favorites", Page.FAVORITES, 0),
            new Nav("CLIENT", null, 1), new Nav("HUD Editor", null, 2), new Nav("Appearance", Page.APPEARANCE, 0),
            new Nav("Configs", Page.CONFIGS, 0), new Nav("Profiles", Page.PROFILES, 0),
            new Nav("Keybinds", Page.KEYBINDS, 0), new Nav("Info", Page.INFO, 0));

    private static final String[] WELCOME = {
            "Ready to enter the void.", "Your minecraft, your rules.", "Smooth frames, sharp aim.",
            "Good luck, have fun.", "Tune everything. Break nothing.", "The void is calling.",
            "Built for speed and style.", "One hub to rule them all."};

    private static final int[] PRESETS = {0xA855F7, 0x3B82F6, 0xEF4444, 0x22C55E, 0xFACC15, 0xFFFFFF, 0xF97316, 0x06B6D4};

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Anim> anims = new HashMap<>();
    private final Set<ClientModule> expanded = new HashSet<>();
    private final Set<Setting<?>> open = new HashSet<>();
    private final Anim scroll = new Anim(0), pageAnim = new Anim(1), openAnim = new Anim(0);

    private Page page = Page.HOME;
    private String search = "", welcome, configName = "", profileName = "";
    private boolean enabledOnly;
    private float scrollTarget, sideScroll, contentH, sideContentH;
    private Drag drag;
    private String tooltip;
    private int mxx, myy, clipTop = Integer.MIN_VALUE, clipBottom = Integer.MAX_VALUE;
    private boolean hitsEnabled = true;
    private int px, py, pw, ph;
    private Object listening;
    private IntConsumer listenSetter;
    private Object focusOwner;
    private Supplier<String> fGet;
    private Consumer<String> fSet;
    private int fMax;
    private List<String> configCache = new ArrayList<>();

    private float[] ppx, ppy, pvy, psz, pph;
    private long lastFrame = System.nanoTime();

    public MenuScreen(Screen parent) {
        super(Component.literal("VOIDNEX"));
        this.parent = parent;
    }

    // =============================================================================================
    // lifecycle
    // =============================================================================================

    @Override protected void init() {
        if (welcome == null) welcome = WELCOME[new Random().nextInt(WELCOME.length)];
        if (ppx == null) {
            int n = 70;
            ppx = new float[n]; ppy = new float[n]; pvy = new float[n]; psz = new float[n]; pph = new float[n];
            Random r = new Random();
            for (int i = 0; i < n; i++) {
                ppx[i] = r.nextFloat(); ppy[i] = r.nextFloat();
                pvy[i] = 0.01f + r.nextFloat() * 0.03f;
                psz[i] = 1 + r.nextInt(2);
                pph[i] = r.nextFloat() * 6.28f;
            }
        }
        Notifications.suppressHud = minecraft != null && minecraft.level != null;
        refreshConfigs();
        openAnim.value = 0;
        openAnim.target = 1;
    }

    @Override public void removed() {
        Notifications.suppressHud = false;
        Config.save();
    }

    @Override public void onClose() {
        Config.save();
        minecraft.gui.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }

    private void refreshConfigs() { configCache = new ArrayList<>(Config.listConfigs()); }

    private void setPage(Page p) {
        page = p;
        search = "";
        scrollTarget = 0;
        scroll.value = 0;
        pageAnim.value = 0;
        pageAnim.target = 1;
        if (p == Page.CONFIGS) refreshConfigs();
    }

    private void openEditor() {
        if (minecraft.level == null || minecraft.player == null) {
            Notifications.push("HUD Editor", "Join a world first to edit your HUD.", Notifications.Type.WARN);
            return;
        }
        minecraft.gui.setScreen(new HudEditorScreen(this));
    }

    // =============================================================================================
    // helpers
    // =============================================================================================

    private Anim a(String kind, Object key) { return anims.computeIfAbsent(kind + key.hashCode(), k -> new Anim(0)); }

    private boolean over(int x, int y, int w, int h) {
        return mxx >= x && mxx < x + w && myy >= y && myy < y + h && myy >= clipTop && myy < clipBottom;
    }

    private void hit(int x, int y, int w, int h, Click action) {
        if (!hitsEnabled) return;
        int y1 = Math.max(y, clipTop), y2 = Math.min(y + h, clipBottom);
        if (y2 <= y1) return;
        hits.add(new Hit(x, y1, w, y2 - y1, action));
    }

    private int tw(String s) { return font.width(s); }
    private void txt(GuiGraphicsExtractor c, String s, int x, int y, int col) { c.text(font, s, x, y, col, false); }

    private void big(GuiGraphicsExtractor c, String s, int x, int y, float sc, int col) {
        var m = c.pose();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(sc, sc);
        c.text(font, s, 0, 0, col, false);
        m.popMatrix();
    }

    private void tip(Setting<?> s, int x, int y, int w, int h) { if (over(x, y, w, h)) tooltip = s.description; }

    private void focus(Object owner, Supplier<String> g, Consumer<String> s, int max) {
        focusOwner = owner; fGet = g; fSet = s; fMax = max;
    }

    private boolean caret() { return (System.currentTimeMillis() / 500) % 2 == 0; }

    private boolean button(GuiGraphicsExtractor c, String label, int x, int y, int w, int h, boolean primary, Click click) {
        boolean hov = over(x, y, w, h);
        Anim an = a("btn", label + x + y);
        an.target = hov ? 1 : 0;
        an.update(18);
        int base = primary ? Theme.accent() : Theme.card();
        int col = primary ? Theme.mix(base, 0xFFFFFFFF, an.value * 0.25f) : Theme.mix(base, Theme.cardHover(), an.value);
        RenderUtil.rounded(c, x, y, w, h, Theme.radius(), col);
        txt(c, label, x + (w - tw(label)) / 2, y + (h - 8) / 2, primary ? 0xFFFFFFFF : Theme.text());
        hit(x, y, w, h, click);
        return hov;
    }

    private void drawSwitch(GuiGraphicsExtractor c, int x, int y, float t) {
        RenderUtil.rounded(c, x, y, 26, 12, 6, Theme.mix(Theme.track(), Theme.accent(), t));
        RenderUtil.rounded(c, x + 2 + Math.round(14 * t), y + 2, 8, 8, 4, 0xFFFFFFFF);
    }

    // =============================================================================================
    // frame
    // =============================================================================================

    @Override public void extractBackground(GuiGraphicsExtractor c, int mouseX, int mouseY, float delta) {
        boolean inWorld = minecraft.level != null;
        if (inWorld && ClientSettings.blur.on()) super.extractBackground(c, mouseX, mouseY, delta);
        drawBackdrop(c, inWorld);
        if (ClientSettings.particles.on()) drawParticles(c);
    }

    private void drawBackdrop(GuiGraphicsExtractor c, boolean inWorld) {
        int top, bot;
        switch (ClientSettings.background.get()) {
            case "Aurora" -> { top = 0xFF04142B; bot = 0xFF0A4D4A; }
            case "Crimson" -> { top = 0xFF16050B; bot = 0xFF4A0E1C; }
            case "Matrix" -> { top = 0xFF010A04; bot = 0xFF053516; }
            default -> { top = 0xFF0A0614; bot = 0xFF21103D; }
        }
        if (!ClientSettings.dark.on()) {
            top = Theme.mix(top, 0xFFEFEEFA, 0.82f);
            bot = Theme.mix(bot, 0xFFDAD6F5, 0.7f);
        }
        float al = inWorld ? 0.62f * openAnim.value : 1f;
        if (!inWorld && ClientSettings.background.is("Custom Image") && Images.background != null) {
            Images.cover(c, Images.background, 0, 0, width, height, Images.bgW, Images.bgH);
            c.fill(0, 0, width, height, 0x66000000);
        } else {
            c.fillGradient(0, 0, width, height, Theme.alpha(top, al), Theme.alpha(bot, al));
        }
        if (ClientSettings.glow.on()) {
            double t = System.currentTimeMillis() / 1000.0;
            int rgb = Theme.accent() & 0xFFFFFF;
            RenderUtil.blob(c, (int) (width * (0.25 + 0.12 * Math.sin(t / 3))), (int) (height * (0.3 + 0.1 * Math.cos(t / 4))), 90, rgb, 0.012f * al);
            RenderUtil.blob(c, (int) (width * (0.75 + 0.1 * Math.cos(t / 5))), (int) (height * (0.75 + 0.1 * Math.sin(t / 3.5))), 80, rgb, 0.010f * al);
        }
    }

    private void drawParticles(GuiGraphicsExtractor c) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        float t = System.currentTimeMillis() / 1000f;
        int rgb = Theme.accent() & 0xFFFFFF;
        for (int i = 0; i < ppx.length; i++) {
            ppy[i] -= pvy[i] * dt;
            if (ppy[i] < -0.02f) { ppy[i] = 1.02f; ppx[i] = (float) Math.random(); }
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(t * 1.5f + pph[i]));
            int x = (int) (ppx[i] * width), y = (int) (ppy[i] * height), s = (int) psz[i];
            c.fill(x, y, x + s, y + s, (Math.round(tw * 140) << 24) | rgb);
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor c, int mouseX, int mouseY, float delta) {
        mxx = mouseX; myy = mouseY;
        hits.clear();
        tooltip = null;
        openAnim.update(12);
        hitsEnabled = openAnim.value > 0.5f;
        extractBackground(c, mouseX, mouseY, delta);

        pw = Math.min(width - 24, 560);
        ph = Math.min(height - 24, 340);
        px = (width - pw) / 2;
        py = (height - ph) / 2 + Math.round((1 - openAnim.value) * 10);
        int r = Theme.radius() + 2;
        if (ClientSettings.glow.on()) RenderUtil.glow(c, px, py, pw, ph, r, Theme.accent() & 0xFFFFFF, 8, 0.5f);
        RenderUtil.rounded(c, px, py, pw, ph, r, Theme.panel());
        RenderUtil.outline(c, px, py, pw, ph, r, Theme.alpha(Theme.accent(), 0.35f));

        drawHeader(c);
        drawSidebar(c);
        drawContent(c);

        Notifications.render(c);
        if (tooltip != null) drawTooltip(c);
    }

    private void drawTooltip(GuiGraphicsExtractor c) {
        String s = font.plainSubstrByWidth(tooltip, 220);
        int w = tw(s) + 10, x = Math.min(mxx + 10, width - w - 4), y = Math.min(myy + 12, height - 18);
        RenderUtil.rounded(c, x, y, w, 15, 4, 0xF0000000);
        txt(c, s, x + 5, y + 4, 0xFFFFFFFF);
    }

    // =============================================================================================
    // header
    // =============================================================================================

    private void drawHeader(GuiGraphicsExtractor c) {
        int accent = Theme.accent();
        if (ClientSettings.customLogo.on() && Images.logo != null) {
            Images.draw(c, Images.logo, px + 10, py + 7, 20, 20, Images.logoW, Images.logoH);
        } else {
            RenderUtil.rounded(c, px + 10, py + 7, 20, 20, 5, accent);
            RenderUtil.rounded(c, px + 12, py + 9, 16, 16, 4, Theme.mix(accent, 0xFF000000, 0.35f));
            big(c, "V", px + 17, py + 11, 1.3f, 0xFFFFFFFF);
        }
        // gradient bold wordmark
        String word = "VOIDNEX";
        int lx = px + 38;
        var m = c.pose();
        m.pushMatrix();
        m.translate((float) lx, (float) (py + 7));
        m.scale(1.3f, 1.3f);
        int cx = 0;
        for (int i = 0; i < word.length(); i++) {
            Component t = Component.literal(String.valueOf(word.charAt(i))).withStyle(ChatFormatting.BOLD);
            int col = Theme.mix(accent, Theme.text(), i / (float) (word.length() - 1));
            c.text(font, t, cx, 0, col, false);
            cx += font.width(t);
        }
        m.popMatrix();
        txt(c, "CLIENT", lx + 1, py + 22, Theme.sub());

        // close
        int cxBtn = px + pw - 26;
        boolean hovClose = over(cxBtn, py + 8, 18, 18);
        RenderUtil.rounded(c, cxBtn, py + 8, 18, 18, Theme.radius(), hovClose ? 0xFFEF4444 : Theme.card());
        txt(c, "x", cxBtn + 6, py + 13, hovClose ? 0xFFFFFFFF : Theme.sub());
        hit(cxBtn, py + 8, 18, 18, (mx, b) -> onClose());

        // search
        int sw = Math.min(150, pw / 3), sx = px + pw - sw - 34;
        boolean f = focusOwner == "search";
        RenderUtil.rounded(c, sx, py + 9, sw, 16, Theme.radius(), f ? Theme.accentSoft() : Theme.card());
        if (f) RenderUtil.outline(c, sx, py + 9, sw, 16, Theme.radius(), accent);
        if (search.isEmpty() && !f) txt(c, "Search modules...", sx + 7, py + 13, Theme.sub());
        else txt(c, font.plainSubstrByWidth(search, sw - 14, true) + (f && caret() ? "|" : ""), sx + 7, py + 13, Theme.text());
        hit(sx, py + 9, sw, 16, (mx, b) -> focus("search", () -> search, v -> { search = v; scrollTarget = 0; }, 32));

        c.fill(px + 10, py + 34, px + pw - 10, py + 35, Theme.line());
    }

    // =============================================================================================
    // sidebar
    // =============================================================================================

    private void drawSidebar(GuiGraphicsExtractor c) {
        int x = px + 8, y = py + 40, w = 104, h = ph - 48;
        c.enableScissor(x, y, x + w, y + h);
        clipTop = y; clipBottom = y + h;
        int cy = y - (int) sideScroll;
        for (Nav n : NAV) {
            if (n.kind() == 1) {
                txt(c, n.label(), x + 6, cy + 6, Theme.sub());
                cy += 17;
                continue;
            }
            boolean sel = n.kind() == 0 && n.page() == page && search.isEmpty();
            boolean hov = over(x, cy, w, 18);
            Anim an = a("nav", n.label());
            an.target = sel ? 1 : hov ? 0.5f : 0;
            an.update(16);
            if (an.value > 0.01f) RenderUtil.rounded(c, x, cy, w, 18, Theme.radius(), Theme.alpha(Theme.accent(), 0.2f * an.value));
            if (sel) RenderUtil.rounded(c, x, cy + 4, 2, 10, 1, Theme.accent());
            txt(c, n.label(), x + 9, cy + 5, Theme.mix(Theme.sub(), Theme.text(), an.value));
            if (n.kind() == 0 && n.page().cat != null) {
                List<ClientModule> l = ModuleManager.byCategory(n.page().cat);
                long on = l.stream().filter(ClientModule::isEnabled).count();
                String badge = on + "/" + l.size();
                txt(c, badge, x + w - tw(badge) - 5, cy + 5, Theme.sub());
            }
            Nav nn = n;
            hit(x, cy, w, 18, (mx, b) -> { if (nn.kind() == 2) openEditor(); else setPage(nn.page()); });
            cy += 19;
        }
        sideContentH = cy + (int) sideScroll - y;
        c.disableScissor();
        clipTop = Integer.MIN_VALUE; clipBottom = Integer.MAX_VALUE;
        c.fill(px + 116, py + 40, px + 117, py + ph - 8, Theme.line());
    }

    // =============================================================================================
    // content frame
    // =============================================================================================

    private void drawContent(GuiGraphicsExtractor c) {
        int x = px + 124, y = py + 40, w = pw - 124 - 10, h = ph - 48;
        pageAnim.update(14);
        c.enableScissor(x, y, x + w, y + h);
        clipTop = y; clipBottom = y + h;
        int start = y + 2 - Math.round(scroll.value) + Math.round((1 - pageAnim.value) * 12);
        int end;
        if (!search.isEmpty()) {
            String q = search;
            end = drawModules(c, x, start, w, ModuleManager.all().stream().filter(m -> m.matches(q)).toList(), "Results for \"" + search + "\"", "No module matches your search.");
        } else {
            end = switch (page) {
                case HOME -> drawHome(c, x, start, w);
                case FAVORITES -> drawModules(c, x, start, w, ModuleManager.all().stream().filter(m -> m.favorite).toList(), "Favorites", "Click the star on a module to pin it here.");
                case APPEARANCE -> drawAppearance(c, x, start, w);
                case CONFIGS -> drawStore(c, x, start, w, false);
                case PROFILES -> drawStore(c, x, start, w, true);
                case KEYBINDS -> drawKeybinds(c, x, start, w);
                case INFO -> drawInfo(c, x, start, w);
                default -> drawModules(c, x, start, w, ModuleManager.byCategory(page.cat), page.cat.label, "No modules.");
            };
        }
        contentH = end - start + 6;
        c.disableScissor();
        clipTop = Integer.MIN_VALUE; clipBottom = Integer.MAX_VALUE;

        float max = Math.max(0f, contentH - h);
        scrollTarget = Mth.clamp(scrollTarget, 0f, max);
        scroll.target = scrollTarget;
        scroll.update(18);
        if (max > 0) {
            int th = Math.max(16, (int) (h * h / (float) (h + max)));
            int ty = y + (int) ((h - th) * (scroll.value / max));
            RenderUtil.rounded(c, x + w + 3, ty, 3, th, 1, Theme.alpha(Theme.accent(), 0.6f));
        }
    }

    // =============================================================================================
    // module cards
    // =============================================================================================

    private int drawModules(GuiGraphicsExtractor c, int x, int y, int w, List<ClientModule> list, String title, String emptyMsg) {
        big(c, title, x, y + 2, 1.2f, Theme.text());
        button(c, "All", x + w - 104, y - 1, 44, 16, !enabledOnly, (mx, b) -> enabledOnly = false);
        button(c, "Enabled", x + w - 56, y - 1, 50, 16, enabledOnly, (mx, b) -> enabledOnly = true);
        y += 24;
        int shown = 0;
        for (ClientModule m : list) {
            if (enabledOnly && !m.isEnabled()) continue;
            y = drawModule(c, m, x, y, w - 8) + 4;
            shown++;
        }
        if (shown == 0) {
            txt(c, enabledOnly ? "No enabled modules here." : emptyMsg, x, y + 4, Theme.sub());
            y += 20;
        }
        return y + 4;
    }

    private int settingHeight(Setting<?> s) {
        if (s instanceof NumberSetting) return 26;
        if (s instanceof ModeSetting ms) return 20 + (open.contains(s) ? ms.options.length * 14 + 4 : 0);
        if (s instanceof ColorSetting) return 20 + (open.contains(s) ? 56 : 0);
        return 20;
    }

    private int settingsHeight(ClientModule m) {
        int h = 6 + 22;
        for (Setting<?> s : m.settings) if (s.isVisible()) h += settingHeight(s);
        if (m instanceof HudModule) h += 24;
        return h + 4;
    }

    private int drawModule(GuiGraphicsExtractor c, ClientModule m, int x, int y, int w) {
        final int hh = 28;
        Anim exp = a("exp", m);
        exp.target = expanded.contains(m) ? 1 : 0;
        exp.update(16);
        Anim en = a("en", m);
        en.target = m.isEnabled() ? 1 : 0;
        en.update(14);
        Anim hv = a("hv", m);
        hv.target = over(x, y, w, hh) ? 1 : 0;
        hv.update(18);
        int extra = Math.round(settingsHeight(m) * exp.value);
        int r = Theme.radius();

        RenderUtil.rounded(c, x, y, w, hh + extra, r, Theme.mix(Theme.card(), Theme.cardHover(), hv.value));
        if (en.value > 0.01f) RenderUtil.rounded(c, x, y + 5, 3, hh - 10, 1, Theme.alpha(Theme.accent(), en.value));
        txt(c, m.name, x + 11, y + 6, Theme.text());
        txt(c, font.plainSubstrByWidth(m.description, w - 120), x + 11, y + 17, Theme.sub());

        hit(x, y, w, hh, (mx, b) -> {
            if (b == 1) m.toggle();
            else if (!expanded.remove(m)) expanded.add(m);
        });
        txt(c, expanded.contains(m) ? "v" : ">", x + w - 80, y + 10, Theme.sub());
        txt(c, m.favorite ? "\u2605" : "\u2606", x + w - 62, y + 10, m.favorite ? 0xFFFFC857 : Theme.sub());
        hit(x + w - 66, y + 4, 16, 20, (mx, b) -> m.favorite = !m.favorite);
        drawSwitch(c, x + w - 38, y + 8, en.value);
        hit(x + w - 42, y + 6, 34, 16, (mx, b) -> m.toggle());

        if (extra > 2) {
            c.enableScissor(x, y + hh, x + w, y + hh + extra);
            boolean prev = hitsEnabled;
            hitsEnabled = prev && exp.value > 0.98f;
            drawSettings(c, m, x, y + hh, w);
            hitsEnabled = prev;
            c.disableScissor();
        }
        return y + hh + extra;
    }

    private void keyChip(GuiGraphicsExtractor c, int rightX, int y, Object owner, IntConsumer setter, int key) {
        boolean lis = listening == owner;
        String s = lis ? "Press a key..." : "[" + Keys.name(key) + "]";
        int w = tw(s) + 12, x = rightX - w;
        RenderUtil.rounded(c, x, y, w, 16, Math.max(0, Theme.radius() - 1), lis ? Theme.accentSoft() : over(x, y, w, 16) ? Theme.cardHover() : Theme.track());
        txt(c, s, x + 6, y + 4, lis ? Theme.accent() : Theme.text());
        hit(x, y, w, 16, (mx, b) -> {
            if (b == 1) setter.accept(-1);
            else { listening = owner; listenSetter = setter; }
        });
    }

    private void drawSettings(GuiGraphicsExtractor c, ClientModule m, int x, int y, int w) {
        int ix = x + 12, iw = w - 24, cy = y + 6;
        txt(c, "Keybind", ix, cy + 5, Theme.text());
        keyChip(c, ix + iw, cy, m, k -> m.key = k, m.key);
        cy += 22;
        for (Setting<?> s : m.settings) {
            if (s.isVisible()) cy += drawSetting(c, s, ix, cy, iw);
        }
        if (m instanceof HudModule hm) {
            button(c, "Reset Position & Scale", ix, cy + 2, 130, 18, false, (mx, b) -> hm.resetPosition());
        }
    }

    // =============================================================================================
    // generic setting rows (used by modules, appearance and update settings)
    // =============================================================================================

    private int drawSetting(GuiGraphicsExtractor c, Setting<?> s, int x, int y, int w) {
        int r = Math.max(0, Theme.radius() - 1);
        if (s instanceof BoolSetting bs) {
            txt(c, bs.name, x, y + 6, Theme.text());
            Anim an = a("sw", bs);
            an.target = bs.on() ? 1 : 0;
            an.update(16);
            drawSwitch(c, x + w - 26, y + 4, an.value);
            hit(x, y, w, 20, (mx, b) -> bs.toggle());
            tip(s, x, y, w, 20);
            return 20;
        }
        if (s instanceof NumberSetting ns) {
            txt(c, ns.name, x, y + 3, Theme.text());
            String v = ns.display();
            txt(c, v, x + w - tw(v), y + 3, Theme.accent());
            int by = y + 16;
            RenderUtil.rounded(c, x, by, w, 4, 2, Theme.track());
            int fw = Math.max(4, (int) (w * ns.progress()));
            RenderUtil.rounded(c, x, by, fw, 4, 2, Theme.accent());
            RenderUtil.rounded(c, x + fw - 3, by - 2, 6, 8, 3, 0xFFFFFFFF);
            final int sx = x, sw = w;
            hit(x, y + 11, w, 14, (mx, b) -> {
                Drag d = m2 -> ns.setProgress((m2 - sx) / (double) sw);
                drag = d;
                d.run(mx);
            });
            tip(s, x, y, w, 26);
            return 26;
        }
        if (s instanceof ModeSetting ms) {
            txt(c, ms.name, x, y + 6, Theme.text());
            boolean op = open.contains(ms);
            String cur = ms.get() + (op ? " ^" : " v");
            int cw = Math.max(70, tw(cur) + 14), cx = x + w - cw;
            RenderUtil.rounded(c, cx, y + 1, cw, 17, r, over(cx, y + 1, cw, 17) ? Theme.cardHover() : Theme.track());
            txt(c, cur, cx + 7, y + 6, Theme.text());
            hit(cx, y + 1, cw, 17, (mx, b) -> { if (!open.remove(ms)) open.add(ms); });
            int hh = 20;
            if (op) {
                for (int i = 0; i < ms.options.length; i++) {
                    final String opt = ms.options[i];
                    int oy = y + 21 + i * 14;
                    boolean sel = ms.is(opt);
                    RenderUtil.rounded(c, cx, oy, cw, 14, 3, sel ? Theme.accentSoft() : over(cx, oy, cw, 14) ? Theme.track() : Theme.card());
                    txt(c, opt, cx + 7, oy + 3, sel ? Theme.accent() : Theme.text());
                    hit(cx, oy, cw, 14, (mx, b) -> { ms.set(opt); open.remove(ms); });
                }
                hh += ms.options.length * 14 + 4;
            }
            tip(s, x, y, w - cw, 20);
            return hh;
        }
        if (s instanceof ColorSetting cs) {
            txt(c, cs.name, x, y + 6, Theme.text());
            RenderUtil.rounded(c, x + w - 26, y + 3, 26, 13, 3, cs.argb());
            RenderUtil.outline(c, x + w - 26, y + 3, 26, 13, 3, Theme.alpha(0xFFFFFFFF, 0.4f));
            hit(x + w - 28, y, 30, 20, (mx, b) -> { if (!open.remove(cs)) open.add(cs); });
            tip(s, x, y, w - 30, 20);
            int hh = 20;
            if (open.contains(cs)) {
                hsvBar(c, x, y + 22, w, cs, 0);
                hsvBar(c, x, y + 34, w, cs, 1);
                hsvBar(c, x, y + 46, w, cs, 2);
                for (int i = 0; i < PRESETS.length; i++) {
                    final int p = PRESETS[i];
                    int sx = x + i * 14;
                    RenderUtil.rounded(c, sx, y + 60, 10, 10, 2, 0xFF000000 | p);
                    hit(sx, y + 60, 10, 10, (mx, b) -> cs.set(p));
                }
                String hex = String.format("#%06X", cs.get());
                txt(c, hex, x + w - tw(hex), y + 61, Theme.sub());
                hh += 56;
            }
            return hh;
        }
        if (s instanceof StringSetting ss) {
            txt(c, ss.name, x, y + 6, Theme.text());
            int fw = Math.max(90, w / 2), bx = x + w - fw;
            boolean f = focusOwner == ss;
            RenderUtil.rounded(c, bx, y + 1, fw, 17, r, f ? Theme.accentSoft() : Theme.track());
            if (f) RenderUtil.outline(c, bx, y + 1, fw, 17, r, Theme.accent());
            txt(c, font.plainSubstrByWidth(ss.get() + (f && caret() ? "|" : ""), fw - 12, true), bx + 6, y + 6, Theme.text());
            hit(bx, y + 1, fw, 17, (mx, b) -> focus(ss, ss::get, ss::set, ss.max));
            tip(s, x, y, w - fw, 20);
            return 20;
        }
        return 0;
    }

    /** mode 0 = hue, 1 = saturation, 2 = brightness. */
    private void hsvBar(GuiGraphicsExtractor c, int x, int y, int w, ColorSetting cs, int mode) {
        final int n = 24, h = 10;
        for (int i = 0; i < n; i++) {
            float t = i / (n - 1f);
            int rgb = switch (mode) {
                case 0 -> Mth.hsvToRgb(t, 1f, 1f);
                case 1 -> Mth.hsvToRgb(cs.h(), t, cs.v());
                default -> Mth.hsvToRgb(cs.h(), cs.s(), t);
            };
            c.fill(x + i * w / n, y, x + (i + 1) * w / n, y + h, 0xFF000000 | rgb);
        }
        float val = mode == 0 ? cs.h() : mode == 1 ? cs.s() : cs.v();
        int kx = x + Math.round(val * (w - 1));
        c.fill(kx - 1, y - 1, kx + 2, y + h + 1, 0xFFFFFFFF);
        c.fill(kx, y, kx + 1, y + h, 0xFF000000);
        final int sx = x;
        hit(x, y, w, h, (mx, b) -> {
            Drag d = m2 -> {
                float t = Mth.clamp((float) (m2 - sx) / (w - 1), 0f, 1f);
                if (mode == 0) cs.setHsv(t, cs.s(), cs.v());
                else if (mode == 1) cs.setHsv(cs.h(), t, cs.v());
                else cs.setHsv(cs.h(), cs.s(), t);
            };
            drag = d;
            d.run(mx);
        });
    }

    // =============================================================================================
    // pages
    // =============================================================================================

    private void stat(GuiGraphicsExtractor c, int x, int y, int w, String label, String value, int col) {
        RenderUtil.rounded(c, x, y, w, 42, Theme.radius(), Theme.card());
        txt(c, label, x + 10, y + 7, Theme.sub());
        big(c, value, x + 10, y + 19, 1.5f, col);
    }

    private int drawHome(GuiGraphicsExtractor c, int x, int y, int fullW) {
        int w = fullW - 8, r = Theme.radius();
        RenderUtil.rounded(c, x, y, w, 56, r, Theme.card());
        RenderUtil.rounded(c, x, y + 8, 3, 40, 1, Theme.accent());
        big(c, "Welcome back, " + minecraft.getUser().getName(), x + 12, y + 8, 1.2f, Theme.text());
        txt(c, welcome, x + 12, y + 25, Theme.accent());
        txt(c, "Minecraft " + minecraft.getLaunchedVersion() + "  |  Fabric  |  " + ModuleManager.all().size()
                + " modules, " + ModuleManager.enabledCount() + " active", x + 12, y + 39, Theme.sub());
        y += 64;

        int cw = (w - 16) / 3, fps = minecraft.getFps(), ping = Info.ping();
        stat(c, x, y, cw, "FPS", String.valueOf(fps), fps >= 60 ? 0xFF55FF55 : fps >= 30 ? 0xFFFFFF55 : 0xFFFF5555);
        stat(c, x + cw + 8, y, cw, "PING", ping < 0 ? "--" : ping + " ms", ping < 0 ? Theme.sub() : ping <= 80 ? 0xFF55FF55 : ping <= 160 ? 0xFFFFFF55 : 0xFFFF5555);
        stat(c, x + 2 * (cw + 8), y, cw, "CPS  (L | R)", Cps.left() + " | " + Cps.right(), Theme.accent());
        y += 52;

        txt(c, "PINNED MODULES", x, y, Theme.sub());
        y += 13;
        List<ClientModule> favs = ModuleManager.all().stream().filter(m -> m.favorite).toList();
        if (favs.isEmpty()) {
            txt(c, "Star a module (\u2606) to pin a quick toggle here.", x, y + 2, Theme.sub());
            y += 18;
        } else {
            int cx = x, cy = y;
            for (ClientModule m : favs) {
                int chipW = tw(m.name) + 26;
                if (cx + chipW > x + w) { cx = x; cy += 21; }
                boolean on = m.isEnabled();
                RenderUtil.rounded(c, cx, cy, chipW, 17, r, on ? Theme.accentSoft() : over(cx, cy, chipW, 17) ? Theme.cardHover() : Theme.card());
                RenderUtil.rounded(c, cx + 7, cy + 6, 5, 5, 2, on ? Theme.accent() : Theme.track());
                txt(c, m.name, cx + 17, cy + 4, on ? Theme.text() : Theme.sub());
                hit(cx, cy, chipW, 17, (mx, b) -> m.toggle());
                cx += chipW + 6;
            }
            y = cy + 25;
        }

        txt(c, "QUICK ACTIONS", x, y, Theme.sub());
        y += 13;
        int bw = (w - 16) / 3;
        button(c, "HUD Editor", x, y, bw, 22, true, (mx, b) -> openEditor());
        button(c, ClientSettings.dark.on() ? "Light Theme" : "Dark Theme", x + bw + 8, y, bw, 22, false, (mx, b) -> ClientSettings.dark.toggle());
        button(c, "Check Updates", x + 2 * (bw + 8), y, bw, 22, false, (mx, b) -> UpdateChecker.check(true));
        return y + 30;
    }

    private int drawAppearance(GuiGraphicsExtractor c, int x, int y, int fullW) {
        int w = fullW - 8;
        big(c, "Appearance", x, y + 2, 1.2f, Theme.text());
        txt(c, "Themes, accent colors, effects and notifications.", x, y + 20, Theme.sub());
        y += 34;
        int total = 12;
        for (Setting<?> s : ClientSettings.APPEARANCE) if (s.isVisible()) total += settingHeight(s);
        total += 52;
        RenderUtil.rounded(c, x, y, w, total, Theme.radius(), Theme.card());
        int cy = y + 6, ix = x + 12, iw = w - 24;
        for (Setting<?> s : ClientSettings.APPEARANCE) if (s.isVisible()) cy += drawSetting(c, s, ix, cy, iw);
        cy += 6;
        button(c, "Reload Custom Images", ix, cy, 130, 18, false, (mx, b) -> {
            boolean ok = Images.reload();
            Notifications.push("Custom images", ok ? "Loaded from config/voidnex." : "No logo.png / background.png found.", ok ? Notifications.Type.SUCCESS : Notifications.Type.WARN);
        });
        button(c, "Open Folder", ix + 136, cy, 80, 18, false, (mx, b) -> openFolder());
        txt(c, "Put logo.png and background.png in config/voidnex/", ix, cy + 24, Theme.sub());
        return y + total + 6;
    }

    private void openFolder() {
        try {
            java.nio.file.Files.createDirectories(Config.dir());
            Util.getPlatform().openFile(Config.dir().toFile());
        } catch (Exception e) {
            Notifications.push("Folder", "Could not open " + Config.dir(), Notifications.Type.ERROR);
        }
    }

    private int drawStore(GuiGraphicsExtractor c, int x, int y, int fullW, boolean profiles) {
        int w = fullW - 8, r = Theme.radius();
        big(c, profiles ? "Module Profiles" : "Config Manager", x, y + 2, 1.2f, Theme.text());
        txt(c, profiles ? "Quick snapshots of module states and settings." : "Full configs saved as files in config/voidnex/configs.", x, y + 20, Theme.sub());
        y += 34;

        String owner = profiles ? "pname" : "cname";
        boolean f = focusOwner == owner;
        String cur = profiles ? profileName : configName;
        int fw = w - 76;
        RenderUtil.rounded(c, x, y, fw, 20, r, f ? Theme.accentSoft() : Theme.card());
        if (f) RenderUtil.outline(c, x, y, fw, 20, r, Theme.accent());
        if (cur.isEmpty() && !f) txt(c, "Name...", x + 8, y + 6, Theme.sub());
        else txt(c, font.plainSubstrByWidth(cur, fw - 16, true) + (f && caret() ? "|" : ""), x + 8, y + 6, Theme.text());
        hit(x, y, fw, 20, (mx, b) -> focus(owner, () -> profiles ? profileName : configName, t -> { if (profiles) profileName = t; else configName = t; }, 24));
        button(c, "Save", x + fw + 6, y, 70, 20, true, (mx, b) -> {
            String n = Config.clean(profiles ? profileName : configName);
            if (n.isEmpty()) { Notifications.push(profiles ? "Profiles" : "Configs", "Type a name first.", Notifications.Type.WARN); return; }
            if (profiles) { Config.profiles.put(n, Config.snapshotModules()); Config.save(); }
            else if (!Config.saveNamed(n)) { Notifications.push("Configs", "Could not save.", Notifications.Type.ERROR); return; }
            refreshConfigs();
            Notifications.push(profiles ? "Profile saved" : "Config saved", n, Notifications.Type.SUCCESS);
        });
        y += 30;

        List<String> names = profiles ? new ArrayList<>(Config.profiles.keySet()) : configCache;
        if (names.isEmpty()) {
            txt(c, "Nothing saved yet.", x, y + 4, Theme.sub());
            y += 20;
        }
        for (String name : names) {
            RenderUtil.rounded(c, x, y, w, 24, r, Theme.card());
            txt(c, name, x + 10, y + 8, Theme.text());
            button(c, "Load", x + w - 106, y + 3, 44, 18, false, (mx, b) -> {
                boolean ok;
                if (profiles) { Config.applyModules(Config.profiles.get(name)); Config.save(); ok = true; }
                else ok = Config.loadNamed(name);
                Notifications.push(ok ? "Loaded" : "Load failed", name, ok ? Notifications.Type.SUCCESS : Notifications.Type.ERROR);
            });
            button(c, "Delete", x + w - 56, y + 3, 50, 18, false, (mx, b) -> {
                if (profiles) { Config.profiles.remove(name); Config.save(); }
                else { Config.deleteNamed(name); refreshConfigs(); }
            });
            y += 28;
        }
        if (!profiles) {
            y += 4;
            button(c, "Open Folder", x, y, 90, 20, false, (mx, b) -> openFolder());
            y += 24;
        }
        return y;
    }

    private int drawKeybinds(GuiGraphicsExtractor c, int x, int y, int fullW) {
        int w = fullW - 8, r = Theme.radius();
        big(c, "Keybind Manager", x, y + 2, 1.2f, Theme.text());
        txt(c, "Click a key to rebind, right-click to clear. Conflicts are marked.", x, y + 20, Theme.sub());
        y += 34;
        Map<Integer, Integer> count = new HashMap<>();
        for (ClientModule m : ModuleManager.all()) if (m.key >= 0) count.merge(m.key, 1, Integer::sum);

        RenderUtil.rounded(c, x, y, w, 24, r, Theme.card());
        txt(c, "Open Menu", x + 10, y + 8, Theme.text());
        String mk = "[" + VoidnexClient.menuKey.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT) + "]";
        txt(c, mk, x + w - tw(mk) - 10, y + 8, Theme.accent());
        txt(c, "change in Options > Controls", x + w - tw(mk) - tw("change in Options > Controls") - 20, y + 8, Theme.sub());
        y += 28;

        for (ClientModule m : ModuleManager.all()) {
            RenderUtil.rounded(c, x, y, w, 24, r, Theme.card());
            txt(c, m.name, x + 10, y + 8, Theme.text());
            txt(c, m.category.label, x + 10 + tw(m.name) + 8, y + 8, Theme.sub());
            keyChip(c, x + w - 8, y + 4, m, k -> m.key = k, m.key);
            if (m.key >= 0 && count.getOrDefault(m.key, 0) > 1) txt(c, "conflict", x + w - 130, y + 8, 0xFFEF4444);
            y += 28;
        }
        return y;
    }

    private void kv(GuiGraphicsExtractor c, int x, int y, String label, String value) {
        txt(c, label, x, y, Theme.sub());
        txt(c, font.plainSubstrByWidth(value, 280), x + 100, y, Theme.text());
    }

    private int drawInfo(GuiGraphicsExtractor c, int x, int y, int fullW) {
        int w = fullW - 8, r = Theme.radius();
        big(c, "About VOIDNEX CLIENT", x, y + 2, 1.2f, Theme.text());
        txt(c, "A modular Fabric minecraft: hub, modules, HUD editor, configs.", x, y + 20, Theme.sub());
        y += 34;

        String loader = FabricLoader.getInstance().getModContainer("fabricloader").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");
        int ping = Info.ping();
        String[][] rows = {
                {"Version", UpdateChecker.currentVersion()}, {"Minecraft", minecraft.getLaunchedVersion()}, {"Fabric Loader", loader},
                {"Java", System.getProperty("java.version")}, {"OS", System.getProperty("os.name")},
                {"Modules", ModuleManager.all().size() + " (" + ModuleManager.enabledCount() + " enabled)"},
                {"Config", Config.dir().toString()}};
        int h1 = rows.length * 13 + 14;
        RenderUtil.rounded(c, x, y, w, h1, r, Theme.card());
        for (int i = 0; i < rows.length; i++) kv(c, x + 12, y + 8 + i * 13, rows[i][0], rows[i][1]);
        y += h1 + 8;

        String[][] acc = {
                {"Username", minecraft.getUser().getName()},
                {"UUID", minecraft.player != null ? minecraft.player.getStringUUID() : "join a world to show"},
                {"Server", Info.server()}, {"Ping", ping < 0 ? "--" : ping + " ms"}};
        int h2 = acc.length * 13 + 14;
        RenderUtil.rounded(c, x, y, w, h2, r, Theme.card());
        for (int i = 0; i < acc.length; i++) kv(c, x + 12, y + 8 + i * 13, acc[i][0], acc[i][1]);
        y += h2 + 8;

        int total = 12;
        for (Setting<?> s : ClientSettings.UPDATES) total += settingHeight(s);
        total += 30;
        RenderUtil.rounded(c, x, y, w, total, r, Theme.card());
        int cy = y + 6;
        for (Setting<?> s : ClientSettings.UPDATES) cy += drawSetting(c, s, x + 12, cy, w - 24);
        button(c, "Check For Updates", x + 12, cy + 4, 120, 18, true, (mx, b) -> UpdateChecker.check(true));
        return y + total + 6;
    }

    // =============================================================================================
    // input
    // =============================================================================================

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        listening = null;
        focusOwner = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x() && mx < h.x() + h.w() && my >= h.y() && my < h.y() + h.h()) {
                h.action().run(mx, button);
                return true;
            }
        }
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (drag != null) { drag.run(event.x()); return true; }
        return false;
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        drag = null;
        Config.save();
        return true;
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (mx < px + 116) {
            float max = Math.max(0f, sideContentH - (ph - 48));
            sideScroll = Mth.clamp(sideScroll - (float) vertical * 18f, 0f, max);
        } else {
            scrollTarget -= (float) vertical * 28f;
        }
        return true;
    }

    @Override public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (listening != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { listening = null; return true; }
            boolean clear = key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE;
            listenSetter.accept(clear ? -1 : key);
            listening = null;
            Config.save();
            return true;
        }
        if (focusOwner != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                focusOwner = null;
                Config.save();
                return true;
            }
            String cur = fGet.get();
            if (key == GLFW.GLFW_KEY_BACKSPACE && !cur.isEmpty()) fSet.accept(cur.substring(0, cur.length() - 1));
            else if (key == GLFW.GLFW_KEY_V && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clip = minecraft.keyboardHandler.getClipboard();
                String joined = cur + clip.replaceAll("[\\r\\n]", "");
                fSet.accept(joined.length() > fMax ? joined.substring(0, fMax) : joined);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || VoidnexClient.menuKey.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public boolean charTyped(CharacterEvent event) {
        if (focusOwner != null) {
            int cp = event.codepoint();
            if (cp >= 32 && cp != 127) {
                String cur = fGet.get();
                if (cur.length() < fMax) fSet.accept(cur + Character.toString(cp));
            }
            return true;
        }
        return false;
    }
}
