package com.voidnex.client.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches the JSON at ClientSettings.updateUrl and compares its "version" with the installed one.
 * Expected format: {"version": "1.0.1", "url": "https://download.page", "notes": "optional"}
 */
public final class UpdateChecker {
    private UpdateChecker() {}

    public static String currentVersion() {
        return FabricLoader.getInstance().getModContainer("voidnex")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    }

    public static void check(boolean manual) {
        String url = ClientSettings.updateUrl.get().trim();
        if (url.isEmpty()) {
            if (manual) Notifications.push("Update Checker", "Set an Update URL on the Info page first.", Notifications.Type.WARN);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "VoidnexClient/" + currentVersion()).GET().build();
            HttpClient.newHttpClient().sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> {
                try {
                    JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
                    String latest = o.get("version").getAsString();
                    boolean newer = compare(latest, currentVersion()) > 0;
                    mc.execute(() -> {
                        if (newer) Notifications.push("Update available", "VOIDNEX " + latest + (o.has("url") ? " - " + o.get("url").getAsString() : ""), Notifications.Type.SUCCESS);
                        else if (manual) Notifications.push("Update Checker", "You are on the latest version.", Notifications.Type.INFO);
                    });
                } catch (Exception e) {
                    mc.execute(() -> { if (manual) Notifications.push("Update Checker", "Invalid response from update URL.", Notifications.Type.ERROR); });
                }
            }).exceptionally(t -> {
                mc.execute(() -> { if (manual) Notifications.push("Update Checker", "Could not reach the update URL.", Notifications.Type.ERROR); });
                return null;
            });
        } catch (Exception e) {
            if (manual) Notifications.push("Update Checker", "The Update URL is not valid.", Notifications.Type.ERROR);
        }
    }

    /** Dotted-number comparison ("1.10.0" > "1.9.2"). */
    static int compare(String a, String b) {
        String[] x = a.split("[^0-9]+"), y = b.split("[^0-9]+");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length && !x[i].isEmpty() ? Integer.parseInt(x[i]) : 0;
            int q = i < y.length && !y[i].isEmpty() ? Integer.parseInt(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }
}
