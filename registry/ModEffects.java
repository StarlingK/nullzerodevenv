package dev.nullmod.registry;

import dev.nullmod.NullMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, NullMod.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> BULLET_TIME = EFFECTS.register("bullet_time",
            () -> new NullEffect(MobEffectCategory.BENEFICIAL, 0x00E5FF));

    public static final DeferredHolder<MobEffect, MobEffect> GHOST = EFFECTS.register("ghost",
            () -> new NullEffect(MobEffectCategory.BENEFICIAL, 0xFF2BD6)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "effect.ghost.speed"),
                            0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> TIME_STOP = EFFECTS.register("time_stop",
            () -> new NullEffect(MobEffectCategory.BENEFICIAL, 0xC4CCD8));

    public static final DeferredHolder<MobEffect, MobEffect> WITHDRAWAL = EFFECTS.register("withdrawal",
            () -> new NullEffect(MobEffectCategory.HARMFUL, 0x5A1E6E));

    public static final DeferredHolder<MobEffect, MobEffect> TEMPORAL_DRAG = EFFECTS.register("temporal_drag",
            () -> new NullEffect(MobEffectCategory.HARMFUL, 0x3A7CA5)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "effect.temporal_drag.speed"),
                            -0.7, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_SPEED,
                            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "effect.temporal_drag.attack"),
                            -0.7, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> TIME_STOPPED = EFFECTS.register("time_stopped",
            () -> new NullEffect(MobEffectCategory.HARMFUL, 0xC4CCD8)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "effect.time_stopped.speed"),
                            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH,
                            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "effect.time_stopped.jump"),
                            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_SPEED,
                            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "effect.time_stopped.attack"),
                            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED,
                            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "effect.time_stopped.mining"),
                            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> REWIND = EFFECTS.register("rewind",
            () -> new NullEffect(MobEffectCategory.BENEFICIAL, 0xFF2BD6)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "effect.rewind.speed"),
                            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH,
                            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "effect.rewind.jump"),
                            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private ModEffects() {}
}
