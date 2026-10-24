package dev.nullmod.registry;

import dev.nullmod.NullMod;
import dev.nullmod.entity.ChronosEchoEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, NullMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ChronosEchoEntity>> ECHO = ENTITIES.register("echo",
            () -> EntityType.Builder.<ChronosEchoEntity>of(ChronosEchoEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("echo"));

    private ModEntities() {}
}
