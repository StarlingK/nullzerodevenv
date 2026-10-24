package dev.nullmod.entity;

import dev.nullmod.core.AbilityManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.level.Level;

public class ChronosPillagerCaptain extends Pillager {
    public static final int SLOW_TICKS = 40;
    private static final float TRIGGER = 0.3f;

    private boolean slowUsed;

    public ChronosPillagerCaptain(EntityType<? extends Pillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Pillager.createAttributes().add(Attributes.MAX_HEALTH, 40.0);
    }

    public void makeCaptain() {
        this.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance(this.registryAccess().lookupOrThrow(
                net.minecraft.core.registries.Registries.BANNER_PATTERN)));
        this.setDropChance(EquipmentSlot.HEAD, 2.0f);
        this.setPatrolLeader(true);
        this.setHealth(this.getMaxHealth());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!slowUsed && this.isAlive() && this.getHealth() <= this.getMaxHealth() * TRIGGER
                && this.level() instanceof ServerLevel level) {
            slowUsed = true;
            AbilityManager.startMobZone(this, SLOW_TICKS);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.HOSTILE, 1.5f, 0.5f);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SlowUsed", slowUsed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        slowUsed = tag.getBoolean("SlowUsed");
    }
}
