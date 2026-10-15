package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.setting.NumberSetting;
import dev.nitro.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

/** Draws boxes (through walls) around storage blocks in loaded chunks. */
public class StorageESP extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 96, 16, 256, 8));
    private final BooleanSetting fill = add(new BooleanSetting("Fill", true));
    private final BooleanSetting tracers = add(new BooleanSetting("Tracers", false));
    private final BooleanSetting chests = add(new BooleanSetting("Chests", true));
    private final BooleanSetting enders = add(new BooleanSetting("Ender chests", true));
    private final BooleanSetting shulkers = add(new BooleanSetting("Shulkers", true));
    private final BooleanSetting barrels = add(new BooleanSetting("Barrels", true));

    private record Entry(Box box, int color) {}
    private List<Entry> cache = List.of();
    private int tick;

    public StorageESP() { super("StorageESP", "Highlights chests, shulkers, barrels through walls.", Category.RENDER); }

    @Override
    public void onDisable() { cache = List.of(); }

    @Override
    public void onEnable() { tick = 0; }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (tick++ % 10 != 0) return;

        double r = range.get();
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        int cr = (int) Math.ceil(r / 16.0) + 1;
        int pcx = mc.player.getChunkPos().x, pcz = mc.player.getChunkPos().z;
        List<Entry> out = new ArrayList<>();

        for (int cx = pcx - cr; cx <= pcx + cr; cx++) {
            for (int cz = pcz - cr; cz <= pcz + cr; cz++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    int color = colorOf(be);
                    if (color == 0) continue;
                    if (be.getPos().getSquaredDistance(px, py, pz) > r * r) continue;
                    out.add(new Entry(new Box(be.getPos()), color));
                }
            }
        }
        cache = out;
    }

    private int colorOf(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return chests.get() ? 0xFFFF4040 : 0;
        if (be instanceof ChestBlockEntity) return chests.get() ? 0xFFFFA500 : 0;
        if (be instanceof EnderChestBlockEntity) return enders.get() ? 0xFFB06BFF : 0;
        if (be instanceof ShulkerBoxBlockEntity) return shulkers.get() ? 0xFFFF6BD6 : 0;
        if (be instanceof BarrelBlockEntity) return barrels.get() ? 0xFFC49A6C : 0;
        return 0;
    }

    @Override
    public void onRender(WorldRenderContext ctx) {
        if (cache.isEmpty()) return;
        RenderUtil.Batch batch = new RenderUtil.Batch(ctx);
        Vec3d start = batch.crosshair();
        for (Entry e : cache) {
            batch.outline(e.box(), e.color());
            if (fill.get()) batch.fill(e.box(), (e.color() & 0x00FFFFFF) | 0x35000000);
            if (tracers.get()) batch.line(start, e.box().getCenter(), e.color());
        }
        batch.draw();
    }
}
