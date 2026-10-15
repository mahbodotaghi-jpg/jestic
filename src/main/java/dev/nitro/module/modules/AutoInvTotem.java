package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.setting.NumberSetting;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/** While your inventory is open: moves a totem into the offhand (and optionally a chosen hotbar slot). */
public class AutoInvTotem extends Module {
    private final BooleanSetting offhand = add(new BooleanSetting("Offhand", true));
    private final NumberSetting hotbarSlot = add(new NumberSetting("Hotbar slot (0=off)", 0, 0, 9, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 2, 0, 10, 1));
    private int timer;

    public AutoInvTotem() { super("AutoInvTotem", "Fills offhand/hotbar totems while your inventory is open.", Category.COMBAT); }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null || !(mc.currentScreen instanceof InventoryScreen)) {
            timer = 0;
            return;
        }
        if (timer++ < delay.get()) return;

        int sync = mc.player.playerScreenHandler.syncId;

        if (offhand.get() && !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            int slot = findTotemSlot();
            if (slot != -1) {
                mc.interactionManager.clickSlot(sync, slot, 40, SlotActionType.SWAP, mc.player);
                timer = 0;
                return;
            }
        }

        int hs = (int) Math.round(hotbarSlot.get());
        if (hs > 0 && !mc.player.getInventory().getStack(hs - 1).isOf(Items.TOTEM_OF_UNDYING)) {
            for (int i = 9; i < 36; i++) {
                if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                    mc.interactionManager.clickSlot(sync, i, hs - 1, SlotActionType.SWAP, mc.player);
                    timer = 0;
                    return;
                }
            }
        }
    }

    /** screen-handler slot id of a totem: main inventory first, then hotbar */
    private int findTotemSlot() {
        for (int i = 9; i < 36; i++) if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return 36 + i;
        return -1;
    }
}
