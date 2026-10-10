package dev.syntrix.clienttest.client.visual;

import dev.syntrix.clienttest.client.combat.CrystalAuraModule;
import error.util.render.Render3D;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class VisualWorldRenderer {
    public record Line(Vec3 a, Vec3 b, int color, float width) {}
    public record Box(AABB bounds, int color, boolean fill) {}

    public static void initialize() {}

    public static void render3D() {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        var lines = new ArrayList<Line>();
        var boxes = new ArrayList<Box>();
        CrystalAuraModule.appendGeometry(lines, boxes);

        for (Box box : boxes) {
            Color c = new Color(box.color, true);
            Render3D.drawBox(box.bounds, c, c, box.fill, true, true);
        }
        for (Line line : lines) {
            Color c = new Color(line.color, true);
            Render3D.drawLine(line.a, line.b, c, true);
        }
    }

    private VisualWorldRenderer() {}
}
