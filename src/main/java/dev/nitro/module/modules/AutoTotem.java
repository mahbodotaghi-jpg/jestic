package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.NumberSetting;
import dev.nitro.util.Safe;
import dev.nitro.util.TotemUtil;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/** Keeps a totem in your offhand. With SafeMode on it only uses totems from the hotbar (select + swap hands). */
public class AutoTotem extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 1, 0, 10, 1));
    private int timer;

    public AutoTotem() { super("AutoTotem", "Keeps a totem in your offhand.", Category.COMBAT); }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) { timer = 0; return; }
        if (timer++ < Safe.minDelay((int) Math.round(delay.get()), 1)) return;

        if (Safe.on()) {
            if (TotemUtil.swapFromHotbar()) timer = -4; // wait for the server to confirm
            return;
        }

        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                int slot = i < 9 ? i + 36 : i;
                mc.interactionManager.clickSlot(0, slot, 40, SlotActionType.SWAP, mc.player);
                timer = 0;
                return;
            }
        }
    }
}
