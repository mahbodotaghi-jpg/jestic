package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.setting.NumberSetting;
import dev.nitro.util.InvUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;

/** While you are falling and aiming at a mob/player, switches to the mace in your hotbar (smash attack). */
public class MaceSwap extends Module {
    private final NumberSetting minFall = add(new NumberSetting("Min fall", 1.5, 0.5, 20, 0.5));
    private final BooleanSetting back = add(new BooleanSetting("Switch back", true));
    private int prev = -1;

    public MaceSwap() { super("MaceSwap", "Switches to the mace when you fall onto a target.", Category.COMBAT); }

    @Override
    public void onDisable() { restore(); }

    private void restore() {
        if (prev != -1 && back.get()) InvUtil.selectSlot(prev);
        prev = -1;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.currentScreen != null) return;
        boolean falling = !mc.player.isOnGround() && mc.player.fallDistance >= minFall.get();
        boolean target = mc.crosshairTarget instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity;

        if (falling && target) {
            int mace = InvUtil.findHotbar(Items.MACE);
            int cur = mc.player.getInventory().selectedSlot;
            if (mace != -1 && mace != cur) {
                if (prev == -1) prev = cur;
                InvUtil.selectSlot(mace);
            }
        } else if (prev != -1) {
            restore();
        }
    }
}
