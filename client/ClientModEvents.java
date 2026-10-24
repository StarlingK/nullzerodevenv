package dev.nullmod.client;

import dev.nullmod.NullMod;
import dev.nullmod.registry.ModEntities;
import dev.nullmod.registry.ModPotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = NullMod.MODID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.CHRONOS);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ECHO.get(), EchoRenderer::new);
    }

    @SubscribeEvent
    public static void onGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
                ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "speech"), SpeechOverlay::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "left_bar"), LeftBar::render);
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "slash"), SlashOverlay::render);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "aim"), SpeechOverlay::renderAim);
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> 0xFF000000 | (tint == 0 ? 0x4A5459 : 0x00E5FF),
                dev.nullmod.registry.ModItems.CAPTAIN_EGG.get());
        event.register((stack, tint) -> {
            if (tint > 0) return -1;
            String path = ModPotions.ourPotionPath(stack);
            int rgb = path != null
                    ? ModPotions.colorOf(path)
                    : stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
            return 0xFF000000 | rgb;
        }, Items.POTION);
    }
}
