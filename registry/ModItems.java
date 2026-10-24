package dev.nullmod.registry;

import dev.nullmod.NullMod;
import dev.nullmod.item.RecipePaperItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NullMod.MODID);

    public static final DeferredItem<RecipePaperItem> RECIPE_PAPER =
            ITEMS.registerItem("recipe_paper", RecipePaperItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<DeferredSpawnEggItem> CAPTAIN_EGG =
            ITEMS.register("chronos_pillager_captain_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.CAPTAIN, 0x4A5459, 0x00E5FF, new Item.Properties()));

    private ModItems() {}
}
