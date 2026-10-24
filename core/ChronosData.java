package dev.nullmod.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class ChronosData implements INBTSerializable<CompoundTag> {

    public float blood;

    public int tier;

    public int dependency;

    public int affinity = -1;
    public long lastDoseTick = -1L;

    public long cleanseUntil;
    public long bulletCdUntil;
    public long holdCdUntil;
    public long dragonCdUntil;

    public int withdrawalTicks;
    public boolean cueDone;

    public long bulletEnd;
    public long bulletStart;
    public boolean hunterOn;
    public long ghostEnd;
    public UUID ghostBody;
    public long timeStopEnd;
    public long glitchEnd;
    public long nextGlitch;
    public long nextSpeech;
    public int lastStage;
    public int lastSyncHash;
    public int lastKind;

    public record TrailPoint(double x, double y, double z, float yaw, float pitch) {}
    public final List<TrailPoint> trail = new ArrayList<>();
    public boolean rewinding;
    public int rewindTick;
    public int rewindFrom;
    public long clashGlitchUntil;
    public long impactUntil;
    public UUID clashWith;

    public long cutsceneStart;
    public long cutsceneShaderAt;
    public long forcedUntil;
    public int cutscenePhase;

    public ChronosTier tierEnum() {
        return ChronosTier.byId(tier);
    }

    public boolean bulletActive(long now) {
        return hunterOn || now < bulletEnd;
    }

    public boolean ghostActive() {
        return ghostBody != null;
    }

    public boolean timeStopActive(long now) {
        return now < timeStopEnd;
    }

    public boolean glitching(long now) {
        return now < glitchEnd;
    }

    public boolean cutsceneActive(long now) {
        return cutscenePhase != 0 && now < forcedUntil;
    }

    public boolean forcedVisual(long now) {
        return cutscenePhase >= 3 && now >= cutsceneShaderAt && now < forcedUntil;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Blood", blood);
        tag.putInt("Tier", tier);
        tag.putInt("Dependency", dependency);
        tag.putInt("Affinity", affinity);
        tag.putLong("LastDose", lastDoseTick);
        tag.putLong("CleanseUntil", cleanseUntil);
        tag.putLong("BulletCd", bulletCdUntil);
        tag.putLong("HoldCd", holdCdUntil);
        tag.putLong("DragonCd", dragonCdUntil);
        tag.putInt("WithdrawalTicks", withdrawalTicks);
        tag.putBoolean("CueDone", cueDone);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        blood = tag.getFloat("Blood");
        tier = tag.getInt("Tier");
        dependency = tag.getInt("Dependency");
        affinity = tag.contains("Affinity") ? tag.getInt("Affinity") : -1;
        lastDoseTick = tag.contains("LastDose") ? tag.getLong("LastDose") : -1L;
        cleanseUntil = tag.getLong("CleanseUntil");
        bulletCdUntil = tag.getLong("BulletCd");
        holdCdUntil = tag.getLong("HoldCd");
        dragonCdUntil = tag.getLong("DragonCd");
        withdrawalTicks = tag.getInt("WithdrawalTicks");
        cueDone = tag.getBoolean("CueDone");
    }
}
