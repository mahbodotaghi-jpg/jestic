package dev.nitro.mixin;

import dev.nitro.module.modules.Freecam;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    // While Freecam is on, the server must not see the camera's movement: it keeps your real position.
    // (No "require = 0" on purpose: if this can't apply, the game should fail loudly instead of sending the camera position.)
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void nitro$freecamNoMovement(CallbackInfo ci) {
        if (Freecam.active()) ci.cancel();
    }
}
