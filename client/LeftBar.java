package dev.nullmod.client;

import com.mojang.math.Axis;

import dev.nullmod.NullMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class LeftBar {
    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "textures/gui/chronos_bar_background.png");
    private static final ResourceLocation FILL =
            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "textures/gui/chronos_bar_progress.png");
    private static final int TEX_W = 162, TEX_H = 5;
    private static final int LENGTH = 100;
    private static final int LEFT = 5;

    private LeftBar() {}

    public static void render(GuiGraphics gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator() || !ClientState.hasChronos()) {
            return;
        }
        int bottom = gfx.guiHeight() / 2 + LENGTH / 2;
        float blood = Math.min(1f, Math.max(0f, ClientState.blood));

        gfx.pose().pushPose();
        gfx.pose().translate(LEFT, bottom, 0);
        gfx.pose().mulPose(Axis.ZP.rotationDegrees(-90f));
        gfx.blit(BG, 0, 0, LENGTH, TEX_H, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
        int filled = Math.round(LENGTH * blood);
        if (filled > 0) {
            int texFilled = Math.max(1, Math.round(TEX_W * blood));
            gfx.blit(FILL, 0, 0, filled, TEX_H, 0, 0, texFilled, TEX_H, TEX_W, TEX_H);
        }
        gfx.pose().popPose();

        Font font = mc.font;
        int seconds = (ClientState.cooldownTicks() + 19) / 20;
        String text = Component.translatable("gui.nullmod.cooldown", seconds).getString();
        int color = seconds > 0 ? 0xFFFF2BD6 : 0xFF00E5FF;
        float scale = 0.75f;
        float tx = Math.max(2f, LEFT + TEX_H / 2f - font.width(text) * scale / 2f);
        gfx.pose().pushPose();
        gfx.pose().translate(tx, bottom + 3, 0);
        gfx.pose().scale(scale, scale, 1f);
        gfx.drawString(font, text, 0, 0, color, true);
        gfx.pose().popPose();
    }
}
