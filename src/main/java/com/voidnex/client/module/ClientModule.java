package com.voidnex.client.module;

import com.google.gson.JsonObject;
import com.voidnex.client.VoidnexClient;
import com.voidnex.client.core.ClientSettings;
import com.voidnex.client.core.Notifications;
import com.voidnex.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Base class of every feature. Subclasses add settings with {@link #add} and override the hooks they need. */
public abstract class ClientModule {
    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting<?>> settings = new ArrayList<>();
    public boolean favorite;
    /** GLFW key code, -1 = unbound. */
    public int key = -1;
    boolean keyWasDown;
    private boolean enabled;
    protected final Minecraft mc = Minecraft.getInstance();

    protected ClientModule(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    // ---- lifecycle hooks ----------------------------------------------------------------------
    protected void onEnable() {}
    protected void onDisable() {}
    /** Called every client tick while enabled (also on menus, so null-check mc.player). */
    public void onTick(Minecraft mc) {}
    /** Called every HUD frame while enabled and in a world. */
    public void onHudRender(GuiGraphicsExtractor ctx) {}
    /** If true, the keybind works as hold-to-activate instead of toggle. */
    public boolean hold() { return false; }
    /** Undo side effects when the game closes (does not change the saved enabled state). */
    public void onShutdown() { if (enabled) onDisable(); }

    // ---- state --------------------------------------------------------------------------------
    protected void defaultEnabled() { enabled = true; }
    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled, true); }
    public void setEnabled(boolean v) { setEnabled(v, true); }

    public void setEnabled(boolean v, boolean notify) {
        if (v == enabled) return;
        enabled = v;
        if (ModuleManager.started) {
            try {
                if (v) onEnable(); else onDisable();
            } catch (Throwable t) {
                VoidnexClient.LOG.error("Module {} failed in {}", name, v ? "onEnable" : "onDisable", t);
            }
        }
        if (notify && ClientSettings.notifyToggles.on()) {
            Notifications.push(name, v ? "Enabled" : "Disabled", v ? Notifications.Type.SUCCESS : Notifications.Type.INFO);
        }
    }

    protected <S extends Setting<?>> S add(S s) {
        settings.add(s);
        return s;
    }

    public boolean matches(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(q) || description.toLowerCase(Locale.ROOT).contains(q)
                || category.label.toLowerCase(Locale.ROOT).contains(q);
    }

    // ---- persistence --------------------------------------------------------------------------
    public JsonObject saveState() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("key", key);
        o.addProperty("favorite", favorite);
        JsonObject s = new JsonObject();
        for (Setting<?> st : settings) s.add(st.name, st.save());
        o.add("settings", s);
        return o;
    }

    public void loadState(JsonObject o) {
        try {
            if (o.has("key")) key = o.get("key").getAsInt();
            if (o.has("favorite")) favorite = o.get("favorite").getAsBoolean();
            if (o.has("settings")) {
                JsonObject s = o.getAsJsonObject("settings");
                for (Setting<?> st : settings) {
                    if (!s.has(st.name)) continue;
                    try { st.load(s.get(st.name)); } catch (Exception ignored) { }
                }
            }
            if (o.has("enabled")) setEnabled(o.get("enabled").getAsBoolean(), false);
        } catch (Exception e) {
            VoidnexClient.LOG.warn("Bad saved state for module {}", name, e);
        }
    }
}
