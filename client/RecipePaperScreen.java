package nullmod.client;

import java.util.List;

import nullmod.registry.ModPotions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

public class RecipePaperScreen extends Screen {
    private static final ResourceLocation BOOK = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private static final int INK = 0x000000;

    private int page;
    private PageButton forward;
    private PageButton back;

    public RecipePaperScreen() {
        super(Component.translatable("item.nullmod.recipe_paper"));
    }

    @Override
    protected void init() {
        int x = (this.width - 192) / 2;
        this.forward = this.addRenderableWidget(new PageButton(x + 116, 159, true, b -> setPage(page + 1), true));
        this.back = this.addRenderableWidget(new PageButton(x + 43, 159, false, b -> setPage(page - 1), true));
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 - 100, 196, 200, 20).build());
        setPage(page);
    }

    private void setPage(int p) {
        this.page = p;
        this.forward.visible = p < lastPage();
        this.back.visible = p > 0;
    }

    private List<FormattedCharSequence> noteLines() {
        return this.font.split(Component.translatable("item.nullmod.recipe_paper.text"), 114);
    }

    private int textPages() {
        return Math.max(1, (noteLines().size() + 13) / 14);
    }

    private int lastPage() {
        return textPages();
    }

    private static ItemStack potion(Holder<Potion> potion) {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return stack;
    }

    private void slot(GuiGraphics gfx, int x, int y, ItemStack stack) {
        gfx.fill(x, y, x + 18, y + 18, 0xFF373737);
        gfx.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        gfx.renderItem(stack, x + 1, y + 1);
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0x90000000);
        gfx.blit(BOOK, (this.width - 192) / 2, 2, 0, 0, 192, 192);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        int x = (this.width - 192) / 2;

        if (page < textPages()) {
            List<FormattedCharSequence> lines = noteLines();
            for (int i = 0; i < 14 && page * 14 + i < lines.size(); i++) {
                gfx.drawString(this.font, lines.get(page * 14 + i), x + 36, 34 + i * 9, INK, false);
            }
        } else {
            gfx.drawString(this.font, Component.translatable("gui.nullmod.paper.step1"), x + 36, 24, INK, false);
            row(gfx, x + 34, 38, potion(ModPotions.START), new ItemStack(Items.GHAST_TEAR), potion(ModPotions.CHRONOS_BASE));
            gfx.drawString(this.font, Component.translatable("gui.nullmod.paper.step2"), x + 36, 80, INK, false);
            row(gfx, x + 34, 94, potion(ModPotions.CHRONOS_BASE), new ItemStack(Items.POPPY), potion(ModPotions.CHRONOS));
            List<FormattedCharSequence> note = this.font.split(Component.translatable("gui.nullmod.paper.note"), 114);
            for (int i = 0; i < note.size() && i < 4; i++) {
                gfx.drawString(this.font, note.get(i), x + 36, 128 + i * 9, 0x5A3A1E, false);
            }
        }

        if (page == lastPage()) {
            int sx = (this.width - 192) / 2 + 34;
            hover(gfx, mouseX, mouseY, sx, 38, potion(ModPotions.START), new ItemStack(Items.GHAST_TEAR), potion(ModPotions.CHRONOS_BASE));
            hover(gfx, mouseX, mouseY, sx, 94, potion(ModPotions.CHRONOS_BASE), new ItemStack(Items.POPPY), potion(ModPotions.CHRONOS));
        }
    }

    private void row(GuiGraphics gfx, int x, int y, ItemStack input, ItemStack ingredient, ItemStack result) {
        slot(gfx, x, y, input);
        gfx.drawString(this.font, "+", x + 22, y + 5, INK, false);
        slot(gfx, x + 32, y, ingredient);
        gfx.drawString(this.font, "=", x + 54, y + 5, INK, false);
        slot(gfx, x + 64, y, result);
        gfx.renderItem(new ItemStack(Items.BREWING_STAND), x + 91, y + 1);
    }

    private void hover(GuiGraphics gfx, int mx, int my, int x, int y, ItemStack a, ItemStack b, ItemStack c) {
        ItemStack[] stacks = {a, b, c};
        int[] xs = {x, x + 32, x + 64};
        for (int i = 0; i < 3; i++) {
            if (mx >= xs[i] && mx < xs[i] + 18 && my >= y && my < y + 18) {
                gfx.renderTooltip(this.font, stacks[i], mx, my);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
