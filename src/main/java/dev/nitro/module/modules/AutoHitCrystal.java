package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.util.Safe;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Hit-crystal macro: breaks any end crystal you are aiming at, and (while you hold right click with a
 * crystal) places crystals on the obsidian/bedrock you are aiming at without the vanilla click delay.
 */
public class AutoHitCrystal extends Module {
    private final BooleanSetting hit = add(new BooleanSetting("Hit", true));
    private final BooleanSetting place = add(new BooleanSetting("Place", true));
    private final NumberSetting hitDelay = add(new NumberSetting("Hit delay", 0, 0, 10, 1));
    private final NumberSetting placeDelay = add(new NumberSetting("Place delay", 1, 0, 10, 1));
    private int hitTimer, placeTimer;

    public AutoHitCrystal() { super("AutoHitCrystal", "Auto breaks/places crystals you aim at.", Category.COMBAT); }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (hitTimer > 0) hitTimer--;
        if (placeTimer > 0) placeTimer--;

        if (hit.get() && hitTimer == 0
                && mc.crosshairTarget instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof EndCrystalEntity crystal && crystal.isAlive()) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            hitTimer = Safe.delay((int) Math.round(hitDelay.get()));
            return;
        }

        if (!place.get() || placeTimer > 0 || !mc.options.useKey.isPressed()) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;

        Hand hand;
        if (mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) hand = Hand.MAIN_HAND;
        else if (mc.player.getOffHandStack().isOf(Items.END_CRYSTAL)) hand = Hand.OFF_HAND;
        else return;

        BlockPos p = bhr.getBlockPos();
        BlockState st = mc.world.getBlockState(p);
        if (!st.isOf(Blocks.OBSIDIAN) && !st.isOf(Blocks.BEDROCK)) return;
        if (!mc.world.getBlockState(p.up()).isAir() || !mc.world.getBlockState(p.up(2)).isAir()) return;
        Box space = new Box(p.getX(), p.getY() + 1, p.getZ(), p.getX() + 1, p.getY() + 3, p.getZ() + 1);
        if (!mc.world.getOtherEntities(null, space, e -> !(e instanceof ItemEntity)).isEmpty()) return;

        mc.interactionManager.interactBlock(mc.player, hand, bhr);
        mc.player.swingHand(hand);
        placeTimer = Safe.delay(Safe.minDelay((int) Math.round(placeDelay.get()), 3));
    }
}
