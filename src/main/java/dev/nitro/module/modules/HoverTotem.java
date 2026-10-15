package dev.nitro.module.modules;

import dev.nitro.mixin.HandledScreenAccessor;
import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.NumberSetting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/** In any inventory/container screen: hover a totem and it jumps to your offhand (then optionally to a hotbar slot). */
public class HoverTotem extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 1, 0, 10, 1));
    private final NumberSetting hotbarSlot = add(new NumberSetting("Hotbar slot (0=off)", 0, 0, 9, 1));
    private int timer;

    public HoverTotem() { super("HoverTotem", "Hover a totem in an inventory to equip it.", Category.COMBAT); }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null || !(mc.currentScreen instanceof HandledScreen<?> screen)) {
            timer = 0;
            return;
        }
        if (timer++ < delay.get()) return;

        Slot focused = ((HandledScreenAccessor) screen).nitro$getFocusedSlot();
        if (focused == null || !focused.hasStack() || !focused.getStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        int sync = screen.getScreenHandler().syncId;

        if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            mc.interactionManager.clickSlot(sync, focused.id, 40, SlotActionType.SWAP, mc.player);
            timer = 0;
            return;
        }

        int hot = (int) Math.round(hotbarSlot.get());
        if (hot > 0 && !mc.player.getInventory().getStack(hot - 1).isOf(Items.TOTEM_OF_UNDYING)) {
            mc.interactionManager.clickSlot(sync, focused.id, hot - 1, SlotActionType.SWAP, mc.player);
            timer = 0;
        }
    }
}
