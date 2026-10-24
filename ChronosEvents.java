package dev.nullmod;

import dev.nullmod.core.AbilityManager;
import dev.nullmod.core.ChronosData;
import dev.nullmod.core.ChronosLogic;
import dev.nullmod.registry.ModPotions;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.PotionBrewEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = NullMod.MODID)
public final class ChronosEvents {
    private ChronosEvents() {}

    @SubscribeEvent
    public static void onBrewing(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();

        mix(builder, ModPotions.START, Items.GHAST_TEAR, ModPotions.CHRONOS_BASE);
        mix(builder, ModPotions.CHRONOS_BASE, Items.POPPY, ModPotions.CHRONOS);

        mix(builder, ModPotions.START, Items.ENDER_EYE, ModPotions.CLEANSING_BASE);
        mix(builder, ModPotions.CLEANSING_BASE, Items.GHAST_TEAR, ModPotions.CLEANSING);
    }

    private static void mix(PotionBrewing.Builder builder, Holder<Potion> from, ItemLike ingredient, Holder<Potion> to) {
        builder.addRecipe(
                DataComponentIngredient.of(true, DataComponents.POTION_CONTENTS, new PotionContents(from), Items.POTION),
                Ingredient.of(ingredient),
                bottle(to));
    }

    private static ItemStack bottle(Holder<Potion> potion) {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return stack;
    }

    @SubscribeEvent
    public static void onBrewed(PotionBrewEvent.Post event) {
        for (int i = 0; i < 3; i++) {
            ItemStack stack = event.getItem(i);
            if ((stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))
                    && ModPotions.ourPotionPath(stack) != null) {
                ItemStack plain = new ItemStack(Items.POTION);
                plain.set(DataComponents.POTION_CONTENTS, stack.get(DataComponents.POTION_CONTENTS));
                event.setItem(i, plain);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        AbilityManager.resolve(event.getServer());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (AbilityManager.isRewinding(player)) {
                event.setCanceled(true);
                return;
            }
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                    && ChronosLogic.data(player).ghostActive()) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && ChronosLogic.data(attacker).ghostActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChronosLogic.tick(player);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (AbilityManager.shouldSkipTick(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0f) {
            AbilityManager.onPlayerHurt(player);
        }
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player
                && "chronos".equals(ModPotions.ourPotionPath(event.getItem()))
                && ChronosLogic.lockedOut(player, true)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String path = ModPotions.ourPotionPath(event.getItem());
            if (path != null) {
                ChronosLogic.onDrink(player, path);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {

            ChronosData d = ChronosLogic.data(player);
            d.ghostBody = null;
            d.bulletEnd = 0;
            d.ghostEnd = 0;
            d.timeStopEnd = 0;
            ChronosLogic.syncNow(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChronosLogic.syncNow(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AbilityManager.endAll(player, ChronosLogic.data(player), false);
            ChronosLogic.syncNow(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AbilityManager.endAll(player, ChronosLogic.data(player), false);
            AbilityManager.forget(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        AbilityManager.clearAll();
    }
}
