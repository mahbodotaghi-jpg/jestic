package dev.nitro.module.modules;

import dev.nitro.NitroClient;
import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.util.math.Vec3d;

/**
 * Your player becomes the camera and flies freely, while a copy of you stays at the original spot as your "body".
 * The server never sees the flying: movement packets are blocked (ClientPlayerEntityMixin), so it keeps your real position.
 * Attack/use/drop/swap input is blocked while it's on. Turning it off puts you back where you started.
 */
public class Freecam extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.2, 5.0, 0.1));

    private OtherClientPlayerEntity body;
    private Vec3d savedPos;
    private float savedYaw, savedPitch, oldFlySpeed;
    private boolean oldFlying, oldAllow;

    public Freecam() { super("Freecam", "Fly around freely while your body stays put.", Category.RENDER); }

    /** true while the camera is flying (used by the mixin that blocks movement packets) */
    public static boolean active() {
        if (NitroClient.modules == null) return false;
        Freecam f = NitroClient.modules.get(Freecam.class);
        return f != null && f.isEnabled() && f.body != null;
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) { toggle(); return; }
        ClientPlayerEntity p = mc.player;

        savedPos = p.getPos();
        savedYaw = p.getYaw();
        savedPitch = p.getPitch();

        body = new OtherClientPlayerEntity(mc.world, p.getGameProfile());
        body.copyPositionAndRotation(p);
        body.headYaw = p.headYaw;
        body.bodyYaw = p.bodyYaw;
        body.getInventory().clone(p.getInventory());
        body.setId(-4242);
        mc.world.addEntity(body);

        PlayerAbilities ab = p.getAbilities();
        oldFlying = ab.flying;
        oldAllow = ab.allowFlying;
        oldFlySpeed = ab.getFlySpeed();
    }

    @Override
    public void onDisable() {
        boolean sameWorld = body != null && mc.world != null && body.getWorld() == mc.world;
        if (sameWorld && mc.player != null) {
            PlayerAbilities ab = mc.player.getAbilities();
            ab.flying = oldFlying;
            ab.allowFlying = oldAllow;
            ab.setFlySpeed(oldFlySpeed);
            mc.player.refreshPositionAndAngles(savedPos.x, savedPos.y, savedPos.z, savedYaw, savedPitch);
            mc.player.setVelocity(Vec3d.ZERO);
            mc.player.fallDistance = 0;
        }
        if (sameWorld) mc.world.removeEntity(body.getId(), Entity.RemovalReason.DISCARDED);
        body = null;
    }

    private static void block(KeyBinding k) {
        k.setPressed(false);
        while (k.wasPressed()) { /* drain queued presses so they can't fire */ }
    }

    @Override
    public void onPreTick() {
        block(mc.options.attackKey);
        block(mc.options.useKey);
        block(mc.options.pickItemKey);
        block(mc.options.dropKey);
        block(mc.options.swapHandsKey);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (body == null || body.getWorld() != mc.world) { toggle(); return; }

        PlayerAbilities ab = mc.player.getAbilities();
        ab.allowFlying = true;
        ab.flying = true;
        ab.setFlySpeed(0.05f * speed.get().floatValue());
    }
}
