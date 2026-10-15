package dev.nitro.module;

import dev.nitro.module.modules.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        // combat
        modules.add(new AutoTotem());
        modules.add(new AutoInvTotem());
        modules.add(new TotemOffhand());
        modules.add(new HoverTotem());
        modules.add(new AutoHitCrystal());
        modules.add(new AnchorMacro());
        modules.add(new ShieldBreaker());
        modules.add(new MaceSwap());
        modules.add(new ElytraSwap());
        // render
        modules.add(new StorageESP());
        modules.add(new PlayerESP());
        modules.add(new Freecam());
        // world
        modules.add(new SusChunkFinder());
        // misc
        modules.add(new AutoTool());
        modules.add(new SafeMode());
        get(SafeMode.class).toggle(); // SafeMode starts on
    }

    public List<Module> getAll() { return modules; }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        for (Module m : modules) if (type.isInstance(m)) return (T) m;
        return null;
    }

    public List<Module> getByCategory(Category c) {
        return modules.stream().filter(m -> m.getCategory() == c).toList();
    }

    public List<Module> getEnabled() {
        return modules.stream().filter(Module::isEnabled).toList();
    }

    public void onPreTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        for (Module m : modules) if (m.isEnabled()) m.onPreTick();
    }

    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        for (Module m : modules) {
            if (m.getKey() >= 0) {
                boolean down = InputUtil.isKeyPressed(mc.getWindow().getHandle(), m.getKey());
                if (down && !m.wasDown && mc.currentScreen == null) m.toggle();
                m.wasDown = down;
            }
            if (m.isEnabled()) m.onTick();
        }
    }

    public void onFrameStart() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        for (Module m : modules) if (m.isEnabled()) m.onFrameStart();
    }

    public void onRender(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        for (Module m : modules) if (m.isEnabled()) m.onRender(ctx);
    }
}
