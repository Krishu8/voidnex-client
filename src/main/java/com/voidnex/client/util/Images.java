package com.voidnex.client.util;

import com.voidnex.client.VoidnexClient;
import com.voidnex.client.core.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads the optional custom logo / background from config/voidnex/{logo,background}.png. */
public final class Images {
    public static Identifier logo, background;
    public static int logoW, logoH, bgW, bgH;

    private Images() {}

    public static boolean reload() {
        int[] s = new int[2];
        logo = load("logo.png", "custom_logo", s);
        logoW = s[0];
        logoH = s[1];
        background = load("background.png", "custom_background", s);
        bgW = s[0];
        bgH = s[1];
        return logo != null || background != null;
    }

    private static Identifier load(String file, String id, int[] size) {
        Path p = Config.dir().resolve(file);
        if (!Files.exists(p)) return null;
        try (InputStream in = Files.newInputStream(p)) {
            NativeImage img = NativeImage.read(in);
            size[0] = img.getWidth();
            size[1] = img.getHeight();
            Identifier ident = Identifier.fromNamespaceAndPath("voidnex", id);
            Minecraft.getInstance().getTextureManager().register(ident, new DynamicTexture(() -> "voidnex_" + id, img));
            return ident;
        } catch (Exception e) {
            VoidnexClient.LOG.warn("Could not load {}", file, e);
            return null;
        }
    }

    /** Stretch the whole texture into the rectangle. */
    public static void draw(GuiGraphicsExtractor c, Identifier id, int x, int y, int w, int h, int tw, int th) {
        c.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, w, h, tw, th, tw, th);
    }

    /** Scale the texture to cover the rectangle, cropping the overflow. */
    public static void cover(GuiGraphicsExtractor c, Identifier id, int x, int y, int w, int h, int tw, int th) {
        float sr = (float) w / h, ir = (float) tw / th;
        int rw = tw, rh = th, u = 0, v = 0;
        if (ir > sr) {
            rw = Math.round(th * sr);
            u = (tw - rw) / 2;
        } else {
            rh = Math.round(tw / sr);
            v = (th - rh) / 2;
        }
        c.blit(RenderPipelines.GUI_TEXTURED, id, x, y, (float) u, (float) v, w, h, rw, rh, tw, th);
    }
}
