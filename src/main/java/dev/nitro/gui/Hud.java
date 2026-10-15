package dev.nitro.gui;

import dev.nitro.NitroClient;
import dev.nitro.module.Module;
import dev.nitro.module.modules.PlayerESP;
import dev.nitro.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.Comparator;
import java.util.List;

public class Hud {
    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        long t = System.currentTimeMillis();

        // watermark with per-letter gradient
        ctx.getMatrices().push();
        ctx.getMatrices().translate(5, 5, 0);
        ctx.getMatrices().scale(1.6f, 1.6f, 1f);
        int x = 0;
        for (char c : "NITRO".toCharArray()) {
            String s = String.valueOf(c);
            ctx.drawTextWithShadow(tr, s, x, 0, Theme.accent(t, x));
            x += tr.getWidth(s);
        }
        ctx.getMatrices().pop();

        PlayerESP esp = NitroClient.modules.get(PlayerESP.class);
        if (esp != null && esp.isEnabled()) esp.drawList(ctx, tr, t);

        // arraylist
        List<Module> on = NitroClient.modules.getEnabled().stream()
                .sorted(Comparator.comparingInt((Module m) -> tr.getWidth(m.getName())).reversed())
                .toList();
        int w = ctx.getScaledWindowWidth();
        int y = 2, i = 0;
        for (Module m : on) {
            int tw = tr.getWidth(m.getName());
            int color = Theme.accent(t, i * 6);
            ctx.fill(w - tw - 8, y, w, y + 11, 0x90000000);
            ctx.fill(w - 2, y, w, y + 11, color);
            ctx.drawTextWithShadow(tr, m.getName(), w - tw - 5, y + 2, color);
            y += 11; i++;
        }
    }
}
