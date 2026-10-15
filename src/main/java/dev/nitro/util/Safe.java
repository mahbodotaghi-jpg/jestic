package dev.nitro.util;

import dev.nitro.NitroClient;
import dev.nitro.module.modules.SafeMode;

import java.util.concurrent.ThreadLocalRandom;

/** Helpers the modules call so SafeMode can tone them down. All of them are no-ops while SafeMode is off. */
public class Safe {
    private static SafeMode mod() {
        if (NitroClient.modules == null) return null;
        SafeMode m = NitroClient.modules.get(SafeMode.class);
        return m != null && m.isEnabled() ? m : null;
    }

    public static boolean on() { return mod() != null; }

    /** base delay plus a random 0..jitter ticks */
    public static int delay(int base) {
        SafeMode m = mod();
        return m == null ? base : base + ThreadLocalRandom.current().nextInt(m.jitter() + 1);
    }

    public static int minDelay(int base, int min) {
        return on() ? Math.max(base, min) : base;
    }
}
