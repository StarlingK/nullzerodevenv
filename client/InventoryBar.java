package nullmod.client;

import java.util.List;

import nullmod.NullMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public final class InventoryBar {
    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "textures/gui/chronos_bar_background.png");
    private static final ResourceLocation FILL =
            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "textures/gui/chronos_bar_progress.png");
    private static final int BAR_W = 162, BAR_H = 5;
    private static final int BTN = 11;

    private static int btnX, btnY;
    private static boolean btnValid;

    private InventoryBar() {}

    public static boolean hit(double mouseX, double mouseY) {
        return btnValid && mouseX >= btnX && mouseX < btnX + BTN && mouseY >= btnY && mouseY < btnY + BTN;
    }

    public static void render(GuiGraphics gfx, InventoryScreen screen, int mouseX, int mouseY) {
        if (!ClientState.hasChronos()) {
            btnValid = false;
            return;
        }
        Font font = Minecraft.getInstance().font;
        int w = screen.getXSize();
        int x = screen.getGuiLeft();
        int h = 58;
        int y = Math.min(screen.getGuiTop() + screen.getYSize() + 2,
                Minecraft.getInstance().getWindow().getGuiScaledHeight() - h - 2);

        panel(gfx, x, y, w, h);

        Component title = Component.translatable(ClientState.stage > 0 ? "gui.nullmod.withdrawal" : "gui.nullmod.chronos");
        gfx.drawString(font, title, x + 8, y + 6, 0x404040, false);

        btnX = x + w - BTN - 6;
        btnY = y + 5;
        btnValid = true;
        boolean hover = hit(mouseX, mouseY);
        int face = ClientState.showInfo ? 0xFF8B8B8B : (hover ? 0xFFDBDBDB : 0xFFC6C6C6);
        gfx.fill(btnX, btnY, btnX + BTN, btnY + BTN, 0xFF000000);
        gfx.fill(btnX + 1, btnY + 1, btnX + BTN - 1, btnY + BTN - 1, face);
        gfx.fill(btnX + 1, btnY + 1, btnX + BTN - 2, btnY + 2, 0xFFFFFFFF);
        gfx.fill(btnX + 1, btnY + 1, btnX + 2, btnY + BTN - 2, 0xFFFFFFFF);
        gfx.fill(btnX + 2, btnY + BTN - 2, btnX + BTN - 1, btnY + BTN - 1, 0xFF555555);
        gfx.fill(btnX + BTN - 2, btnY + 2, btnX + BTN - 1, btnY + BTN - 1, 0xFF555555);
        gfx.drawString(font, "I", btnX + (BTN - font.width("I")) / 2, btnY + 2, 0x404040, false);

        String percent = Math.round(ClientState.blood * 100f) + "%";
        int tx = x + (w - font.width(percent)) / 2;
        int ty = y + 16;
        int fillColor = ClientState.blood > 0f ? 0x00E5FF : 0xFF2BD6;
        gfx.drawString(font, percent, tx + 1, ty, 0x000000, false);
        gfx.drawString(font, percent, tx - 1, ty, 0x000000, false);
        gfx.drawString(font, percent, tx, ty + 1, 0x000000, false);
        gfx.drawString(font, percent, tx, ty - 1, 0x000000, false);
        gfx.drawString(font, percent, tx, ty, fillColor, false);

        int bx = x + (w - BAR_W) / 2;
        int by = y + 27;
        gfx.blit(BG, bx, by, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H);
        int filled = Math.round(BAR_W * Math.min(1f, Math.max(0f, ClientState.blood)));
        if (filled > 0) {
            gfx.blit(FILL, bx, by, 0, 0, filled, BAR_H, BAR_W, BAR_H);
        }

        Component affinity = ClientState.affinity < 0
                ? Component.translatable("gui.nullmod.affinity.unknown")
                : Component.translatable("gui.nullmod.affinity", ClientState.affinity);
        gfx.drawString(font, affinity, x + 8, y + 39, 0x404040, false);

        if (ClientState.tier != 0) {
            Component type = ClientState.tierEnum().typeLabel();
            int color = ClientState.tierEnum().color;
            int typeX = x + 8 + font.width(affinity) + 8;
            gfx.drawString(font, type, typeX, y + 39, 0xFF000000 | color, color != 0x000000);
        }

        if (ClientState.showInfo) {
            popout(gfx, font, screen);
        }
    }

    private static void popout(GuiGraphics gfx, Font font, InventoryScreen screen) {
        int width = 156;
        List<FormattedCharSequence> body = font.split(ClientState.infoText(), width - 16);
        int height = 8 + 12 + 6 + body.size() * 9 + 8;
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int px = screen.getGuiLeft() + screen.getXSize() + 6;
        if (px + width > screenW) {
            px = screen.getGuiLeft() - width - 6;
        }
        if (px < 2) {
            px = 2;
        }
        int py = screen.getGuiTop();

        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 400);
        panel(gfx, px, py, width, height);
        int color = ClientState.tier == 0 ? 0x404040 : ClientState.tierEnum().color;
        Component title = ClientState.tier == 0
                ? Component.translatable("gui.nullmod.chronos")
                : ClientState.tierEnum().displayName();
        gfx.drawString(font, title, px + 8, py + 8, 0xFF000000 | color, color != 0x000000);
        gfx.fill(px + 8, py + 19, px + width - 8, py + 20, 0xFF8B8B8B);
        for (int i = 0; i < body.size(); i++) {
            gfx.drawString(font, body.get(i), px + 8, py + 26 + i * 9, 0x404040, false);
        }
        gfx.pose().popPose();
    }

    private static void panel(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, 0xFF000000);
        gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        gfx.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
        gfx.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
        gfx.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        gfx.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, 0xFF555555);
    }
}
