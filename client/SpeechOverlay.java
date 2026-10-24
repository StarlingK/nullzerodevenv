package dev.nullmod.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;

public final class SlashOverlay {
    private static final int LIFE = 7;

    private SlashOverlay() {}

    public static void render(GuiGraphics gfx, DeltaTracker delta) {
        if (ClientState.slashAge >= LIFE || Minecraft.getInstance().options.hideGui) {
            return;
        }
        float t = (ClientState.slashAge + delta.getGameTimeDeltaPartialTick(false)) / LIFE;
        float reveal = Mth.clamp(t * 3.0f, 0f, 1f);
        int alpha = Mth.clamp((int) (255f * (1f - t)), 0, 255);
        int w = gfx.guiWidth();
        int h = gfx.guiHeight();
        float length = (float) Math.hypot(w, h) * 1.2f * reveal;

        gfx.pose().pushPose();
        gfx.pose().translate(w / 2f, h / 2f, 0);
        gfx.pose().mulPose(Axis.ZP.rotationDegrees(-28f));
        gfx.fill((int) (-length / 2), -6, (int) (length / 2), 6, (alpha / 3 << 24) | 0x00E5FF);
        gfx.fill((int) (-length / 2), -3, (int) (length / 2), 3, (alpha / 2 << 24) | 0xFF2BD6);
        gfx.fill((int) (-length / 2), -1, (int) (length / 2), 1, (alpha << 24) | 0xFFFFFF);
        gfx.pose().popPose();
    }
}
