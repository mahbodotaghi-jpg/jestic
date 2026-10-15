package dev.nitro.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class RenderUtil {
    /** Collect boxes/lines in world coordinates, then draw everything through walls with draw(). */
    public static class Batch {
        private record BoxJob(Box box, int color) {}
        private record LineJob(Vec3d a, Vec3d b, int color) {}

        private final Matrix4f matrix;
        private final Camera camera;
        private final Vec3d cam;
        private final List<BoxJob> fills = new ArrayList<>(), outlines = new ArrayList<>();
        private final List<LineJob> lines = new ArrayList<>();

        public Batch(WorldRenderContext ctx) {
            this.matrix = ctx.matrixStack().peek().getPositionMatrix();
            this.camera = ctx.camera();
            this.cam = camera.getPos();
        }

        /** a point just in front of the camera, for tracers */
        public Vec3d crosshair() {
            Vec3d look = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
            return cam.add(look.multiply(0.5));
        }

        public void fill(Box b, int argb) { fills.add(new BoxJob(b, argb)); }
        public void outline(Box b, int argb) { outlines.add(new BoxJob(b, argb)); }
        public void line(Vec3d a, Vec3d b, int argb) { lines.add(new LineJob(a, b, argb)); }

        private void v(BufferBuilder b, double x, double y, double z, int c) {
            b.vertex(matrix, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).color(c);
        }

        private void quad(BufferBuilder b, int c, double x1, double y1, double z1, double x2, double y2, double z2,
                          double x3, double y3, double z3, double x4, double y4, double z4) {
            v(b, x1, y1, z1, c); v(b, x2, y2, z2, c); v(b, x3, y3, z3, c); v(b, x4, y4, z4, c);
        }

        private void edge(BufferBuilder b, int c, double x1, double y1, double z1, double x2, double y2, double z2) {
            v(b, x1, y1, z1, c); v(b, x2, y2, z2, c);
        }

        public void draw() {
            if (fills.isEmpty() && outlines.isEmpty() && lines.isEmpty()) return;

            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            if (!fills.isEmpty()) {
                RenderSystem.disableCull();
                BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                for (BoxJob j : fills) {
                    Box x = j.box; int c = j.color;
                    quad(b, c, x.minX, x.minY, x.minZ, x.maxX, x.minY, x.minZ, x.maxX, x.minY, x.maxZ, x.minX, x.minY, x.maxZ);
                    quad(b, c, x.minX, x.maxY, x.minZ, x.maxX, x.maxY, x.minZ, x.maxX, x.maxY, x.maxZ, x.minX, x.maxY, x.maxZ);
                    quad(b, c, x.minX, x.minY, x.minZ, x.minX, x.maxY, x.minZ, x.maxX, x.maxY, x.minZ, x.maxX, x.minY, x.minZ);
                    quad(b, c, x.minX, x.minY, x.maxZ, x.minX, x.maxY, x.maxZ, x.maxX, x.maxY, x.maxZ, x.maxX, x.minY, x.maxZ);
                    quad(b, c, x.minX, x.minY, x.minZ, x.minX, x.maxY, x.minZ, x.minX, x.maxY, x.maxZ, x.minX, x.minY, x.maxZ);
                    quad(b, c, x.maxX, x.minY, x.minZ, x.maxX, x.maxY, x.minZ, x.maxX, x.maxY, x.maxZ, x.maxX, x.minY, x.maxZ);
                }
                BufferRenderer.drawWithGlobalProgram(b.end());
                RenderSystem.enableCull();
            }

            if (!outlines.isEmpty() || !lines.isEmpty()) {
                RenderSystem.lineWidth(2f);
                BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                for (BoxJob j : outlines) {
                    Box x = j.box; int c = j.color;
                    edge(b, c, x.minX, x.minY, x.minZ, x.maxX, x.minY, x.minZ);
                    edge(b, c, x.maxX, x.minY, x.minZ, x.maxX, x.minY, x.maxZ);
                    edge(b, c, x.maxX, x.minY, x.maxZ, x.minX, x.minY, x.maxZ);
                    edge(b, c, x.minX, x.minY, x.maxZ, x.minX, x.minY, x.minZ);
                    edge(b, c, x.minX, x.maxY, x.minZ, x.maxX, x.maxY, x.minZ);
                    edge(b, c, x.maxX, x.maxY, x.minZ, x.maxX, x.maxY, x.maxZ);
                    edge(b, c, x.maxX, x.maxY, x.maxZ, x.minX, x.maxY, x.maxZ);
                    edge(b, c, x.minX, x.maxY, x.maxZ, x.minX, x.maxY, x.minZ);
                    edge(b, c, x.minX, x.minY, x.minZ, x.minX, x.maxY, x.minZ);
                    edge(b, c, x.maxX, x.minY, x.minZ, x.maxX, x.maxY, x.minZ);
                    edge(b, c, x.maxX, x.minY, x.maxZ, x.maxX, x.maxY, x.maxZ);
                    edge(b, c, x.minX, x.minY, x.maxZ, x.minX, x.maxY, x.maxZ);
                }
                for (LineJob l : lines) edge(b, l.color, l.a.x, l.a.y, l.a.z, l.b.x, l.b.y, l.b.z);
                BufferRenderer.drawWithGlobalProgram(b.end());
            }

            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }
}
