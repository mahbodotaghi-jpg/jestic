package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.setting.NumberSetting;
import dev.nitro.util.Safe;
import dev.nitro.util.TotemUtil;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/** Forces a totem into the offhand and can lock the swap-hands key. Inventory clicks only happen while your inventory is open when SafeMode is on. */
public class TotemOffhand extends Module {
    private final BooleanSetting lock = add(new BooleanSetting("Lock swap-hands key", true));
    private final NumberSetting health = add(new NumberSetting("Only below health (0=always)", 0, 0, 20, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 0, 0, 10, 1));
    private int timer;

    public TotemOffhand() { super("TotemOffhand", "Forces a totem into your offhand and locks it there.", Category.COMBAT); }

    @Override
    public void onPreTick() {
        if (!lock.get() || mc.player == null) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            mc.options.swapHandsKey.setPressed(false);
            while (mc.options.swapHandsKey.wasPressed()) { /* swallow F presses */ }
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        boolean inInventory = mc.currentScreen instanceof InventoryScreen;
        if (mc.currentScreen != null && !inInventory) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) { timer = 0; return; }
        if (health.get() > 0 && mc.player.getHealth() + mc.player.getAbsorptionAmount() > health.get()) return;
        if (timer++ < Safe.minDelay((int) Math.round(delay.get()), 1)) return;

        if (Safe.on() && !inInventory) {
            if (TotemUtil.swapFromHotbar()) timer = -4;
            return;
        }

        int slot = -1;
        for (int i = 9; i < 36 && slot == -1; i++) if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) slot = i;
        for (int i = 0; i < 9 && slot == -1; i++) if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) slot = 36 + i;
        if (slot == -1) return;

        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
        timer = 0;
    }
}
