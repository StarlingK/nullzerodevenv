package nullmod.registry;

import nullmod.NullMod;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = NullMod.MODID)
public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NullMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NULL_TAB = TABS.register("null", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nullmod"))
                    .icon(() -> potion(ModPotions.CHRONOS))
                    .displayItems((parameters, output) -> {
                        output.accept(potion(ModPotions.CHRONOS_BASE));
                        output.accept(potion(ModPotions.CHRONOS));
                        output.accept(potion(ModPotions.CLEANSING_BASE));
                        output.accept(potion(ModPotions.CLEANSING));
                        output.accept(ModItems.RECIPE_PAPER.get());
                        output.accept(ModItems.CAPTAIN_EGG.get());
                    })
                    .build());

    @SubscribeEvent
    public static void onTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.FOOD_AND_DRINKS && event.getTabKey() != CreativeModeTabs.COMBAT) return;
        Item[] variants = {Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW};
        java.util.List<Holder<Potion>> ours = java.util.List.of(
                ModPotions.CHRONOS_BASE, ModPotions.CHRONOS, ModPotions.CLEANSING_BASE, ModPotions.CLEANSING);
        for (Item item : variants) {
            for (Holder<Potion> potion : ours) {
                ItemStack stack = new ItemStack(item);
                stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
                event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }

    private static ItemStack potion(Holder<Potion> potion) {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return stack;
    }

    private ModTabs() {}
}
