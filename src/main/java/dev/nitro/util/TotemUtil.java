package dev.nitro.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class TotemUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /** The vanilla way: select the hotbar totem, press "swap hands", select the old slot again. No inventory clicks. */
    public static boolean swapFromHotbar() {
        int slot = InvUtil.findHotbar(Items.TOTEM_OF_UNDYING);
        ClientPlayNetworkHandler net = mc.getNetworkHandler();
        if (slot < 0 || net == null || mc.player == null) return false;

        int cur = mc.player.getInventory().selectedSlot;
        if (slot != cur) net.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        net.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        if (slot != cur) net.sendPacket(new UpdateSelectedSlotC2SPacket(cur));
        return true;
    }
}
