package dev.nitro.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;

public class InvUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /** @return hotbar slot (0-8) holding the item, or -1 */
    public static int findHotbar(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    public static void selectSlot(int slot) {
        if (mc.player != null && slot >= 0 && slot < 9) mc.player.getInventory().selectedSlot = slot;
    }
}
