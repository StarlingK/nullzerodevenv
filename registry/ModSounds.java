package dev.nullmod.registry;

import dev.nullmod.NullMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, NullMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> INJECTION = register("injection");

    public static final DeferredHolder<SoundEvent, SoundEvent> SLOWMO_1 = register("slowmo1");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLOWMO_2 = register("slowmo2");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLOWMO_3 = register("slowmo3");

    public static final DeferredHolder<SoundEvent, SoundEvent> WITHDRAWAL_1 = register("withdrawal1");
    public static final DeferredHolder<SoundEvent, SoundEvent> WITHDRAWAL_2 = register("withdrawal2");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(NullMod.MODID, name)));
    }

    private ModSounds() {}
}
