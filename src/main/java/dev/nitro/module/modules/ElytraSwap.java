package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.BooleanSetting;
import dev.nitro.setting.NumberSetting;
import dev.nitro.util.Safe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.function.Predicate;

/**
 * Swaps your chestplate for an elytra when you fall for a few ticks, and back again when you land.
 * With SafeMode on it only uses items from the hotbar and equips them by right-clicking, like a normal player.
 */
public class ElytraSwap extends Module {
    private final NumberSetting airTicks = add(new NumberSetting("Air ticks", 6, 1, 40, 1));
    private final BooleanSetting swapBack = add(new BooleanSetting("Swap back on land", true));
    private int air, cool;
    private boolean swapped;

    public ElytraSwap() { super("ElytraSwap", "Swaps chestplate and elytra in mid-air.", Category.COMBAT); }

    @Override
    public void onEnable() { air = cool = 0; swapped = false; }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (cool > 0) cool--;
        if (mc.currentScreen != null) return;

        boolean wearingElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        boolean airborne = !mc.player.isOnGround() && !mc.player.isTouchingWater() && !mc.player.isClimbing()
                && !mc.player.getAbilities().flying;
        air = airborne ? air + 1 : 0;
        if (cool > 0) return;

        if (airborne && air >= airTicks.get() && !wearingElytra && mc.player.getVelocity().y < -0.3) {
            if (equip(s -> s.isOf(Items.ELYTRA))) { swapped = true; cool = Safe.delay(10); }
        } else if (!airborne && wearingElytra && swapped && swapBack.get()) {
            if (equip(s -> s.isIn(ItemTags.CHEST_ARMOR))) { swapped = false; cool = Safe.delay(10); }
        }
    }

    private boolean equip(Predicate<ItemStack> wanted) {
        PlayerInventory inv = mc.player.getInventory();

        if (Safe.on()) {
            // right-click the armor piece from the hotbar
            for (int i = 0; i < 9; i++) {
                if (!wanted.test(inv.getStack(i))) continue;
                int cur = inv.selectedSlot;
                inv.selectedSlot = i;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                inv.selectedSlot = cur;
                if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(cur));
                return true;
            }
            return false;
        }

        int sync = mc.player.playerScreenHandler.syncId;
        for (int i = 0; i < 36; i++) {
            if (!wanted.test(inv.getStack(i))) continue;
            if (i < 9) {
                mc.interactionManager.clickSlot(sync, 6, i, SlotActionType.SWAP, mc.player);
            } else {
                int hot = inv.selectedSlot;
                mc.interactionManager.clickSlot(sync, i, hot, SlotActionType.SWAP, mc.player);
                mc.interactionManager.clickSlot(sync, 6, hot, SlotActionType.SWAP, mc.player);
                mc.interactionManager.clickSlot(sync, i, hot, SlotActionType.SWAP, mc.player);
            }
            return true;
        }
        return false;
    }
}
