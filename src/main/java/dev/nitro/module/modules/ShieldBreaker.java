package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.util.InvUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

/** When you aim at a player who is blocking with a shield: switch to an axe (and hit them) to disable it. */
public class ShieldBreaker extends Module {
    private final BooleanSetting autoAttack = add(new BooleanSetting("Auto attack", true));
    private final BooleanSetting back = add(new BooleanSetting("Switch back", true));
    private int prev = -1;

    public ShieldBreaker() { super("ShieldBreaker", "Axes shields: swaps to an axe when your target blocks.", Category.COMBAT); }

    @Override
    public void onDisable() { restore(); }

    private void restore() {
        if (prev != -1 && back.get()) InvUtil.selectSlot(prev);
        prev = -1;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;

        if (mc.crosshairTarget instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof PlayerEntity p && p != mc.player && p.isBlocking()) {
            int axe = -1;
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).isIn(ItemTags.AXES)) { axe = i; break; }
            }
            if (axe == -1) return;

            int cur = mc.player.getInventory().selectedSlot;
            if (axe != cur) {
                if (prev == -1) prev = cur;
                InvUtil.selectSlot(axe);
            }
            if (autoAttack.get() && mc.player.getAttackCooldownProgress(0f) >= 1f) {
                mc.interactionManager.attackEntity(mc.player, p);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else if (prev != -1) {
            restore();
        }
    }
}
