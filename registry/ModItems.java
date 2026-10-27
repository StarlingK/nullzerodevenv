package nullmod.registry;

import nullmod.NullMod;
import nullmod.item.RecipePaperItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NullMod.MODID);

    public static final DeferredItem<RecipePaperItem> RECIPE_PAPER =
            ITEMS.registerItem("recipe_paper", RecipePaperItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<nullmod.item.CaptainEggItem> CAPTAIN_EGG =
            ITEMS.registerItem("chronos_pillager_captain_spawn_egg", nullmod.item.CaptainEggItem::new);

    private ModItems() {}
}
