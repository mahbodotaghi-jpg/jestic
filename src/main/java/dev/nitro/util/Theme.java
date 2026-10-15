package dev.nitro.util;

import java.awt.Color;

public class Theme {
    /** Animated amber accent colour (ARGB). Change BASE_HUE to recolour the whole client (0.12 amber, 0.9 pink, 0.55 cyan). */
    private static final float BASE_HUE = 0.12f;

    public static int accent(long timeMs, int offset) {
        float h = BASE_HUE + 0.025f * (float) Math.sin((timeMs + offset * 40L) / 600.0);
        return Color.HSBtoRGB(h, 0.78f, 1f);
    }

    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }
}
