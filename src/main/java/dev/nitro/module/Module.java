package dev.nitro.module;

import dev.nitro.setting.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name, description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int key = -1;
    boolean wasDown;

    protected Module(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
    }

    protected <S extends Setting<?>> S add(S s) { settings.add(s); return s; }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    /** start of every client tick (before the player ticks) */
    public void onPreTick() {}
    /** end of every client tick */
    public void onTick() {}
    /** start of every rendered frame */
    public void onFrameStart() {}
    /** world render pass (draw ESP here) */
    public void onRender(WorldRenderContext ctx) {}

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return settings; }
    public boolean isEnabled() { return enabled; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
}
