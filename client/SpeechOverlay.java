package dev.nullmod.client;

import java.util.List;

import dev.nullmod.NullMod;
import dev.nullmod.net.SpeechPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class SpeechOverlay {
    public static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "visitor");
    private static final float SCALE = 0.8f;

    private SpeechOverlay() {}

    public static void render(GuiGraphics gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientState.speechKind < 0 || mc.options.hideGui) {
            return;
        }
        Font font = mc.font;
        int kind = ClientState.speechKind;
        boolean neon = kind == SpeechPayload.HIGH_1 || kind == SpeechPayload.NO_WORK || kind == SpeechPayload.CAPTAIN;
        boolean red = kind == SpeechPayload.HIGH_2;

        int left = ClientState.SPEECH_LIFE - ClientState.speechAge;
        int alpha = Mth.clamp((int) (255f * left / 15f), 0, 255);
        if (alpha < 6) return;

        int main = neon ? 0xFF2BD6 : red ? 0xFF2222 : 0xFFFFFF;
        int shadow = neon ? 0x00E5FF : 0x000000;

        Component base = ClientState.speechText();
        Component mainText = base.copy().withStyle(s -> s.withFont(FONT).withColor(main));
        Component shadowText = base.copy().withStyle(s -> s.withFont(FONT).withColor(shadow));

        int right = gfx.guiWidth() / 2 + 91;
        int wrap = Math.max(60, (int) (Math.min(220, right - 8) / SCALE));
        List<FormattedCharSequence> mainLines = font.split(mainText, wrap);
        List<FormattedCharSequence> shadowLines = font.split(shadowText, wrap);

        int lineH = 9;
        float bottom = gfx.guiHeight() - 59;
        float top = bottom - mainLines.size() * lineH * SCALE;
        float shakeX = 0, shakeY = 0;
        if ((neon || red) && mc.level != null) {
            shakeX = (mc.level.random.nextFloat() - 0.5f) * 2f;
            shakeY = (mc.level.random.nextFloat() - 0.5f) * 2f;
        }
        int argb = alpha << 24;

        for (int i = 0; i < mainLines.size(); i++) {
            FormattedCharSequence line = mainLines.get(i);
            float w = font.width(line) * SCALE;
            float x = right - w;
            float y = top + i * lineH * SCALE;
            gfx.pose().pushPose();
            gfx.pose().translate(x + shakeX, y + shakeY, 0);
            gfx.pose().scale(SCALE, SCALE, 1f);
            if (neon) {
                gfx.drawString(font, shadowLines.get(i), 2, 1, argb | shadow, false);
                gfx.drawString(font, shadowLines.get(i), -1, 0, (alpha / 2 << 24) | shadow, false);
            } else {
                gfx.drawString(font, shadowLines.get(i), 1, 1, argb | shadow, false);
            }
            gfx.drawString(font, line, 0, 0, argb | main, false);
            gfx.pose().popPose();
        }
    }

    public static void renderAim(GuiGraphics gfx, DeltaTracker delta) {
        if (!ClientState.aiming) return;
        Minecraft mc = Minecraft.getInstance();
        Component text = Component.translatable("gui.nullmod.range", String.format("%.0f", ClientState.aimRange))
                .withStyle(s -> s.withFont(FONT));
        int w = mc.font.width(text);
        gfx.drawString(mc.font, text, gfx.guiWidth() / 2 - w / 2, gfx.guiHeight() / 2 + 14, 0xFF00E5FF, true);
    }
}
