package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.setting.ModeSetting;
import dev.nitro.setting.NumberSetting;
import dev.nitro.util.RenderUtil;
import dev.nitro.util.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Boxes (through walls) around other players, optional tracers and a nearby-players list on the HUD. */
public class PlayerESP extends Module {
    private final ModeSetting color = add(new ModeSetting("Color", "Accent", "Red", "White"));
    private final NumberSetting range = add(new NumberSetting("Range", 128, 16, 256, 8));
    private final BooleanSetting fill = add(new BooleanSetting("Fill", true));
    private final BooleanSetting tracers = add(new BooleanSetting("Tracers", false));
    private final BooleanSetting list = add(new BooleanSetting("Player list", true));
    private final BooleanSetting self = add(new BooleanSetting("Show self", false));

    public PlayerESP() { super("PlayerESP", "Boxes and a list for nearby players.", Category.RENDER); }

    private int colorNow() {
        return switch (color.get()) {
            case "Red" -> 0xFFFF4444;
            case "White" -> 0xFFFFFFFF;
            default -> Theme.accent(System.currentTimeMillis(), 0);
        };
    }

    @Override
    public void onRender(WorldRenderContext ctx) {
        float td = mc.getRenderTickCounter().getTickDelta(true);
        RenderUtil.Batch batch = new RenderUtil.Batch(ctx);
        Vec3d start = batch.crosshair();
        int c = colorNow();
        boolean any = false;

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p.isSpectator() || (p == mc.player && !self.get())) continue;
            if (p != mc.player && mc.player.distanceTo(p) > range.get()) continue;

            double x = MathHelper.lerp(td, p.prevX, p.getX());
            double y = MathHelper.lerp(td, p.prevY, p.getY());
            double z = MathHelper.lerp(td, p.prevZ, p.getZ());
            Box box = p.getBoundingBox().offset(x - p.getX(), y - p.getY(), z - p.getZ());

            batch.outline(box, c);
            if (fill.get()) batch.fill(box, (c & 0x00FFFFFF) | 0x30000000);
            if (tracers.get() && p != mc.player) batch.line(start, box.getCenter(), c);
            any = true;
        }
        if (any) batch.draw();
    }

    public void drawList(DrawContext ctx, TextRenderer tr, long t) {
        if (!list.get() || mc.world == null || mc.player == null) return;
        List<PlayerEntity> ps = new ArrayList<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && !p.isSpectator()) ps.add(p);
        }
        ps.sort(Comparator.comparingDouble(p -> mc.player.distanceTo(p)));

        int y = 28, i = 0;
        for (PlayerEntity p : ps) {
            if (i++ >= 10) break;
            String line = p.getName().getString() + "  " + (int) mc.player.distanceTo(p) + "m  "
                    + String.format(Locale.ROOT, "%.1f", p.getHealth() + p.getAbsorptionAmount()) + "hp";
            ctx.drawTextWithShadow(tr, line, 6, y, Theme.accent(t, i * 5));
            y += 10;
        }
    }
}
