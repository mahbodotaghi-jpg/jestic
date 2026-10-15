package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.util.InvUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/** Switches to the fastest hotbar tool while you hold attack on a block (and optionally a weapon for mobs/players). */
public class AutoTool extends Module {
    private final BooleanSetting weapons = add(new BooleanSetting("Weapons", false));
    private final BooleanSetting switchBack = add(new BooleanSetting("Switch back", true));
    private int prevSlot = -1;

    public AutoTool() { super("AutoTool", "Picks the best hotbar tool for the block you mine.", Category.MISC); }

    @Override
    public void onDisable() { restore(); }

    private void restore() {
        if (prevSlot != -1 && switchBack.get()) InvUtil.selectSlot(prevSlot);
        prevSlot = -1;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (!mc.options.attackKey.isPressed()) { restore(); return; }
        if (mc.player.isCreative()) return;

        int cur = mc.player.getInventory().selectedSlot;
        HitResult hr = mc.crosshairTarget;

        if (hr instanceof BlockHitResult bhr && hr.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            BlockState st = mc.world.getBlockState(pos);
            if (st.isAir() || st.getHardness(mc.world, pos) < 0) return;

            int best = -1;
            float bestSpeed = mc.player.getInventory().getStack(cur).getMiningSpeedMultiplier(st);
            for (int i = 0; i < 9; i++) {
                float sp = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(st);
                if (sp > bestSpeed) { bestSpeed = sp; best = i; }
            }
            if (best != -1) {
                if (prevSlot == -1) prevSlot = cur;
                InvUtil.selectSlot(best);
            }
        } else if (weapons.get() && hr instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity) {
            int best = -1, bestScore = weaponScore(mc.player.getInventory().getStack(cur));
            for (int i = 0; i < 9; i++) {
                int sc = weaponScore(mc.player.getInventory().getStack(i));
                if (sc > bestScore) { bestScore = sc; best = i; }
            }
            if (best != -1) {
                if (prevSlot == -1) prevSlot = cur;
                InvUtil.selectSlot(best);
            }
        }
    }

    private static int weaponScore(ItemStack s) {
        if (s.isIn(ItemTags.SWORDS)) return 2;
        if (s.isIn(ItemTags.AXES)) return 1;
        return 0;
    }
}
