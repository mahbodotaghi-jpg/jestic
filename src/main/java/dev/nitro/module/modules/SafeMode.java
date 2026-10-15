package dev.nitro.module.modules;

import dev.nitro.module.Category;
import dev.nitro.module.Module;
import dev.nitro.setting.NumberSetting;

/**
 * Legit-style behaviour for the other modules: random extra delay on timers, and vanilla-style totem / armor swaps
 * (hotbar + swap-hands / right-click instead of inventory clicks). Lowers the chance of being flagged, it does not make you undetectable.
 */
public class SafeMode extends Module {
    private final NumberSetting jitter = add(new NumberSetting("Delay jitter (ticks)", 1, 0, 3, 1));

    public SafeMode() {
        super("SafeMode", "Random delays and vanilla-style totem/armor swaps so modules look more like a normal player.", Category.MISC);
    }

    public int jitter() { return (int) Math.round(jitter.get()); }
}
