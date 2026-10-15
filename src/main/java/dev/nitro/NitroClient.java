package dev.nitro;

import dev.nitro.gui.ClickGuiScreen;
import dev.nitro.gui.Hud;
import dev.nitro.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class NitroClient implements ClientModInitializer {
    public static ModuleManager modules;
    private static KeyBinding guiKey;

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        modules.init();

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nitro.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.nitro"));

        ClientTickEvents.START_CLIENT_TICK.register(client -> modules.onPreTick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            modules.onTick();
            while (guiKey.wasPressed()) client.setScreen(new ClickGuiScreen());
        });

        WorldRenderEvents.START.register(ctx -> modules.onFrameStart());
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> modules.onRender(ctx));

        HudRenderCallback.EVENT.register(Hud::render);
    }
}
