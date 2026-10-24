package dev.nullmod;

import dev.nullmod.core.AbilityManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.raid.Raid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = NullMod.MODID)
public final class CaptainEvents {
    private static final String TAG = "nullmod_chronos";
    private static final String USED = "nullmod_chronos_used";
    public static final int SLOW_TICKS = 40;

    private CaptainEvents() {}

    public static boolean isChronos(Pillager pillager) {
        return pillager.getPersistentData().getBoolean(TAG);
    }

    public static void makeChronos(Pillager pillager) {
        pillager.getPersistentData().putBoolean(TAG, true);
        pillager.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance(
                pillager.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BANNER_PATTERN)));
        pillager.setDropChance(EquipmentSlot.HEAD, 2.0f);
        pillager.setPatrolLeader(true);
        pillager.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
        pillager.setHealth(40.0f);
        pillager.setCustomName(Component.translatable("entity.nullmod.chronos_pillager_captain"));
        pillager.setPersistenceRequired();
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity().getType() != EntityType.PILLAGER || event.loadedFromDisk()
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Pillager pillager = (Pillager) event.getEntity();
        if (isChronos(pillager) || pillager.getCurrentRaid() != null
                || level.random.nextDouble() * 100.0 >= NullConfig.CAPTAIN_CHANCE.get()) {
            return;
        }
        makeChronos(pillager);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Pillager pillager) || pillager.level().isClientSide
                || !isChronos(pillager) || !pillager.isAlive()) {
            return;
        }
        if (pillager.getPersistentData().getBoolean(USED)
                || pillager.getHealth() > pillager.getMaxHealth() * 0.3f) {
            return;
        }
        pillager.getPersistentData().putBoolean(USED, true);
        AbilityManager.startMobZone(pillager, SLOW_TICKS);
        pillager.level().playSound(null, pillager.getX(), pillager.getY(), pillager.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5f, 0.5f);
    }
}
