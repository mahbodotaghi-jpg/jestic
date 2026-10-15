package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.util.Safe;
import dev.nitro.setting.NumberSetting;
import dev.nitro.util.InvUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Look at a respawn anchor: charges it with glowstone, then switches to another item and detonates it.
 * (Anchors only explode outside the Nether.)
 */
public class AnchorMacro extends Module {
    private final NumberSetting charges = add(new NumberSetting("Charges", 1, 1, 4, 1));
    private final NumberSetting explodeSlot = add(new NumberSetting("Explode slot", 1, 1, 9, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 1, 0, 10, 1));
    private int timer;

    public AnchorMacro() { super("AnchorMacro", "Charge + explode the respawn anchor you look at.", Category.COMBAT); }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (timer > 0) { timer--; return; }
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockState state = mc.world.getBlockState(hit.getBlockPos());
        if (!state.isOf(Blocks.RESPAWN_ANCHOR)) return;
        int current = state.get(RespawnAnchorBlock.CHARGES);

        if (current < charges.get()) {
            int glow = InvUtil.findHotbar(Items.GLOWSTONE);
            if (glow < 0) return;
            InvUtil.selectSlot(glow);
        } else {
            int slot = (int) Math.round(explodeSlot.get()) - 1;
            if (mc.player.getInventory().getStack(slot).isOf(Items.GLOWSTONE)) {
                slot = -1;
                for (int i = 0; i < 9; i++) {
                    if (!mc.player.getInventory().getStack(i).isOf(Items.GLOWSTONE)) { slot = i; break; }
                }
                if (slot < 0) return;
            }
            InvUtil.selectSlot(slot);
        }

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
        timer = Safe.delay(Safe.minDelay((int) Math.round(delay.get()), 2));
    }
}
