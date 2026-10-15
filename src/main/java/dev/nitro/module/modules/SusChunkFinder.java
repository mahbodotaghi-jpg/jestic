package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.setting.NumberSetting;
import dev.nitro.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Looks through the chunks you have loaded for blocks that rarely generate naturally (signs, concrete, pistons,
 * ender chests, shulkers, planks, glass...). Chunks with enough of them are boxed through walls, announced in chat
 * and logged to .minecraft/nitro/sus_chunks.txt. Villages, mineshafts and other structures can score too.
 */
public class SusChunkFinder extends Module {
    private final NumberSetting minScore = add(new NumberSetting("Min score", 12, 3, 200, 1));
    private final NumberSetting speed = add(new NumberSetting("Chunks per tick", 1, 1, 8, 1));
    private final BooleanSetting notify = add(new BooleanSetting("Chat notify", true));
    private final BooleanSetting log = add(new BooleanSetting("Log to file", true));
    private final BooleanSetting fill = add(new BooleanSetting("Fill", true));
    private final BooleanSetting tracers = add(new BooleanSetting("Tracers", false));

    private static final Map<Block, Integer> WEIGHTS = buildWeights();

    private record Found(int cx, int cz, int score, double y) {}

    private final Map<Long, Found> results = new HashMap<>();
    private final Set<Long> scanned = new HashSet<>();
    private final List<Long> pending = new ArrayList<>();
    private World lastWorld;
    private int tick;

    public SusChunkFinder() {
        super("SusChunkFinder", "Finds chunks with blocks that rarely generate naturally (possible bases).", Category.WORLD);
    }

    private static Map<Block, Integer> buildWeights() {
        Map<Block, Integer> m = new HashMap<>();
        for (Block b : Registries.BLOCK) {
            String p = Registries.BLOCK.getId(b).getPath();
            int w = 0;

            if (p.equals("ender_chest") || p.equals("shulker_box") || p.endsWith("_shulker_box")
                    || p.equals("beacon") || p.equals("tnt") || p.equals("hopper") || p.equals("dropper")
                    || p.equals("dispenser") || p.equals("observer") || p.equals("piston") || p.equals("sticky_piston")
                    || p.equals("repeater") || p.equals("comparator") || p.equals("redstone_lamp") || p.equals("note_block")
                    || p.equals("jukebox") || p.equals("enchanting_table") || p.endsWith("anvil")
                    || p.endsWith("_sign") || p.endsWith("_hanging_sign") || p.endsWith("_concrete")
                    || p.equals("iron_block") || p.equals("gold_block") || p.equals("diamond_block")
                    || p.equals("emerald_block") || p.equals("netherite_block") || p.equals("lapis_block")
                    || p.equals("redstone_block") || p.equals("respawn_anchor") || p.equals("lodestone")
                    || p.equals("conduit") || p.equals("smithing_table") || p.equals("grindstone")
                    || p.equals("loom") || p.equals("stonecutter") || p.equals("cartography_table")
                    || p.equals("brewing_stand")) {
                w = 3;
            } else if (p.equals("trapped_chest")) {
                w = 2;
            } else if (p.endsWith("_planks") || p.endsWith("_wool") || p.endsWith("glass") || p.equals("bricks")
                    || p.equals("crafting_table") || p.equals("furnace") || p.equals("blast_furnace")
                    || p.equals("smoker") || p.equals("bookshelf") || p.equals("barrel") || p.equals("chest")
                    || p.endsWith("_bed") || p.endsWith("lantern") || p.equals("composter")) {
                w = 1;
            }
            if (w > 0) m.put(b, w);
        }
        return m;
    }

    @Override
    public void onEnable() { reset(); }

    @Override
    public void onDisable() { reset(); }

    private void reset() {
        results.clear();
        scanned.clear();
        pending.clear();
        lastWorld = null;
        tick = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (mc.world != lastWorld) { reset(); lastWorld = mc.world; }

        // every so often: queue loaded chunks we haven't scanned yet, nearest first
        if (tick++ % 20 == 0) {
            int r = mc.options.getViewDistance().getValue();
            int pcx = mc.player.getChunkPos().x, pcz = mc.player.getChunkPos().z;
            pending.clear();
            for (int cx = pcx - r; cx <= pcx + r; cx++) {
                for (int cz = pcz - r; cz <= pcz + r; cz++) {
                    long key = ChunkPos.toLong(cx, cz);
                    if (scanned.contains(key)) continue;
                    if (mc.world.getChunkManager().getWorldChunk(cx, cz) == null) continue;
                    pending.add(key);
                }
            }
            pending.sort(Comparator.comparingInt(k -> {
                int dx = ChunkPos.getPackedX(k) - pcx, dz = ChunkPos.getPackedZ(k) - pcz;
                return dx * dx + dz * dz;
            }));
        }

        int n = (int) Math.round(speed.get());
        while (n-- > 0 && !pending.isEmpty()) {
            long key = pending.remove(0);
            scanned.add(key);
            WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(ChunkPos.getPackedX(key), ChunkPos.getPackedZ(key));
            if (chunk != null) scan(chunk, key);
        }
    }

    private void scan(WorldChunk chunk, long key) {
        ChunkSection[] sections = chunk.getSectionArray();
        int bottom = chunk.getBottomY();
        int score = 0;
        double ySum = 0;

        for (int si = 0; si < sections.length; si++) {
            ChunkSection sec = sections[si];
            if (sec == null || sec.isEmpty()) continue;
            int baseY = bottom + si * 16;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Integer w = WEIGHTS.get(sec.getBlockState(x, y, z).getBlock());
                        if (w != null) { score += w; ySum += (double) (baseY + y) * w; }
                    }
                }
            }
        }
        if (score < 3) return;

        int cx = ChunkPos.getPackedX(key), cz = ChunkPos.getPackedZ(key);
        double avgY = ySum / score;
        results.put(key, new Found(cx, cz, score, avgY));

        if (score >= minScore.get()) {
            int bx = cx * 16 + 8, bz = cz * 16 + 8;
            if (notify.get() && mc.player != null) {
                mc.player.sendMessage(Text.literal("\u00A7b[NITRO]\u00A7r Sus chunk (score " + score + ") near X " + bx + " Y " + (int) avgY + " Z " + bz), false);
            }
            if (log.get()) write(score, bx, (int) avgY, bz);
        }
    }

    private void write(int score, int x, int y, int z) {
        try {
            Path file = FabricLoader.getInstance().getGameDir().resolve("nitro").resolve("sus_chunks.txt");
            Files.createDirectories(file.getParent());
            String server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
            String line = LocalDateTime.now() + " | " + server + " | " + mc.world.getRegistryKey().getValue()
                    + " | X " + x + " Y " + y + " Z " + z + " | score " + score + "\n";
            Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    @Override
    public void onRender(WorldRenderContext ctx) {
        if (results.isEmpty()) return;
        RenderUtil.Batch b = new RenderUtil.Batch(ctx);
        var start = b.crosshair();
        boolean any = false;

        for (Found f : results.values()) {
            if (f.score() < minScore.get()) continue;
            float heat = Math.min(1f, f.score() / (float) (minScore.get() * 4));
            int g = (int) (200 * (1f - heat));
            int color = 0xFF000000 | (255 << 16) | (g << 8) | 40;

            Box box = new Box(f.cx() * 16, f.y() - 1, f.cz() * 16, f.cx() * 16 + 16, f.y() + 2, f.cz() * 16 + 16);
            b.outline(box, color);
            if (fill.get()) b.fill(box, (color & 0x00FFFFFF) | 0x30000000);
            if (tracers.get()) b.line(start, box.getCenter(), color);
            any = true;
        }
        if (any) b.draw();
    }
}
