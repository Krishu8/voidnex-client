package com.voidnex.client.module.hud;

import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.setting.NumberSetting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CoordsHud extends TextHud {
    private final NumberSetting decimals = add(new NumberSetting("Decimals", "Digits after the decimal point.", 0, 0, 2, 1));
    private final BoolSetting direction = add(new BoolSetting("Show Direction", "Show the direction you are facing.", true));
    private final BoolSetting biome = add(new BoolSetting("Show Biome", "Show the biome you are standing in.", true));
    private final BoolSetting nether = add(new BoolSetting("Show Nether Coords", "Show the matching Nether / Overworld position.", false));

    public CoordsHud() {
        super("Coordinates", "Shows your position, direction and biome.", 0.01f, 0.19f);
        defaultEnabled();
    }

    private String fmt(double v) { return String.format(Locale.ROOT, "%." + decimals.i() + "f", v); }

    @Override protected List<String> lines(boolean preview) {
        Player p = mc.player;
        List<String> l = new ArrayList<>();
        l.add("XYZ: " + fmt(p.getX()) + " " + fmt(p.getY()) + " " + fmt(p.getZ()));
        if (nether.on() && mc.level != null) {
            boolean inNether = mc.level.dimension() == Level.NETHER;
            double k = inNether ? 8.0 : 1.0 / 8.0;
            l.add((inNether ? "Overworld: " : "Nether: ") + fmt(p.getX() * k) + " " + fmt(p.getZ() * k));
        }
        if (direction.on()) {
            Direction d = p.getDirection();
            String axis = switch (d) {
                case NORTH -> "-Z";
                case SOUTH -> "+Z";
                case EAST -> "+X";
                default -> "-X";
            };
            String n = d.getName();
            l.add("Facing: " + Character.toUpperCase(n.charAt(0)) + n.substring(1) + " (" + axis + ")");
        }
        if (biome.on() && mc.level != null) {
            String b = mc.level.getBiome(p.blockPosition()).unwrapKey().map(k -> k.identifier().getPath()).orElse("unknown");
            l.add("Biome: " + b.replace('_', ' '));
        }
        return l;
    }
}
