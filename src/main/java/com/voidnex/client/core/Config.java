package com.voidnex.client.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.voidnex.client.VoidnexClient;
import com.voidnex.client.module.ClientModule;
import com.voidnex.client.module.ModuleManager;
import com.voidnex.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Persistence. config.json holds everything (client options, modules, profiles).
 * Named configs live in configs/NAME.json and hold client options + modules.
 */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Module profiles: name -> module snapshot. */
    public static final Map<String, JsonObject> profiles = new LinkedHashMap<>();

    private Config() {}

    public static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("voidnex"); }
    private static Path file() { return dir().resolve("config.json"); }
    private static Path configs() { return dir().resolve("configs"); }

    public static String clean(String s) { return s.replaceAll("[^A-Za-z0-9 _-]", "").trim(); }

    // ---- snapshots ----------------------------------------------------------------------------

    public static JsonObject snapshotModules() {
        JsonObject o = new JsonObject();
        for (ClientModule m : ModuleManager.all()) o.add(m.name, m.saveState());
        return o;
    }

    public static void applyModules(JsonObject o) {
        for (ClientModule m : ModuleManager.all()) {
            if (o.has(m.name) && o.get(m.name).isJsonObject()) m.loadState(o.getAsJsonObject(m.name));
        }
    }

    private static JsonObject snapshotClient() {
        JsonObject o = new JsonObject();
        for (Setting<?> s : all()) o.add(s.name, s.save());
        return o;
    }

    private static void applyClient(JsonObject o) {
        for (Setting<?> s : all()) {
            if (!o.has(s.name)) continue;
            try { s.load(o.get(s.name)); } catch (Exception ignored) { }
        }
    }

    private static List<Setting<?>> all() {
        java.util.ArrayList<Setting<?>> l = new java.util.ArrayList<>(ClientSettings.APPEARANCE);
        l.addAll(ClientSettings.UPDATES);
        return l;
    }

    // ---- main config --------------------------------------------------------------------------

    public static void save() {
        try {
            Files.createDirectories(dir());
            JsonObject root = new JsonObject();
            root.add("client", snapshotClient());
            root.add("modules", snapshotModules());
            JsonObject p = new JsonObject();
            profiles.forEach(p::add);
            root.add("profiles", p);
            Files.writeString(file(), GSON.toJson(root));
        } catch (Exception e) {
            VoidnexClient.LOG.error("Could not save config", e);
        }
    }

    public static void load() {
        try {
            if (!Files.exists(file())) return;
            JsonObject root = JsonParser.parseString(Files.readString(file())).getAsJsonObject();
            if (root.has("client")) applyClient(root.getAsJsonObject("client"));
            if (root.has("modules")) applyModules(root.getAsJsonObject("modules"));
            profiles.clear();
            if (root.has("profiles")) {
                root.getAsJsonObject("profiles").entrySet().forEach(e -> profiles.put(e.getKey(), e.getValue().getAsJsonObject()));
            }
        } catch (Exception e) {
            VoidnexClient.LOG.error("Could not load config (using defaults)", e);
        }
    }

    // ---- named configs ------------------------------------------------------------------------

    public static List<String> listConfigs() {
        try {
            if (!Files.isDirectory(configs())) return List.of();
            try (var s = Files.list(configs())) {
                return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
                        .map(n -> n.substring(0, n.length() - 5)).sorted().collect(Collectors.toList());
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    public static boolean saveNamed(String name) {
        try {
            Files.createDirectories(configs());
            JsonObject root = new JsonObject();
            root.add("client", snapshotClient());
            root.add("modules", snapshotModules());
            Files.writeString(configs().resolve(name + ".json"), GSON.toJson(root));
            return true;
        } catch (Exception e) {
            VoidnexClient.LOG.error("Could not save config {}", name, e);
            return false;
        }
    }

    public static boolean loadNamed(String name) {
        try {
            JsonObject root = JsonParser.parseString(Files.readString(configs().resolve(name + ".json"))).getAsJsonObject();
            if (root.has("client")) applyClient(root.getAsJsonObject("client"));
            if (root.has("modules")) applyModules(root.getAsJsonObject("modules"));
            save();
            return true;
        } catch (Exception e) {
            VoidnexClient.LOG.error("Could not load config {}", name, e);
            return false;
        }
    }

    public static void deleteNamed(String name) {
        try { Files.deleteIfExists(configs().resolve(name + ".json")); } catch (Exception ignored) { }
    }
}
