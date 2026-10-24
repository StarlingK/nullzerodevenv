package dev.nullmod.registry;

import dev.nullmod.NullMod;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(BuiltInRegistries.POTION, NullMod.MODID);

    public static final Holder<Potion> START = Potions.THICK;

    public static final DeferredHolder<Potion, Potion> CHRONOS_BASE =
            POTIONS.register("chronos_base", () -> new Potion("nullmod_chronos_base"));
    public static final DeferredHolder<Potion, Potion> CHRONOS =
            POTIONS.register("chronos", () -> new Potion("nullmod_chronos"));
    public static final DeferredHolder<Potion, Potion> CLEANSING_BASE =
            POTIONS.register("cleansing_base", () -> new Potion("nullmod_cleansing_base"));
    public static final DeferredHolder<Potion, Potion> CLEANSING =
            POTIONS.register("cleansing", () -> new Potion("nullmod_cleansing"));

    public static int colorOf(String path) {
        return switch (path) {
            case "chronos_base" -> 0x4B2FBF;
            case "chronos" -> 0x00E5FF;
            case "cleansing_base" -> 0x8FBFA6;
            case "cleansing" -> 0xEAF6F1;
            default -> -1;
        };
    }

    public static String ourPotionPath(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return null;
        return contents.potion()
                .flatMap(Holder::unwrapKey)
                .map(ResourceKey::location)
                .filter(id -> id.getNamespace().equals(NullMod.MODID))
                .map(id -> id.getPath())
                .orElse(null);
    }

    private ModPotions() {}
}
