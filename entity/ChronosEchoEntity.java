package dev.nullmod.entity;

import java.util.Optional;
import java.util.UUID;

import dev.nullmod.core.AbilityManager;
import dev.nullmod.core.ChronosData;
import dev.nullmod.core.ChronosLogic;
import dev.nullmod.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ChronosEchoEntity extends Entity {
    public static final int BODY = 0;
    public static final int TRAIL = 1;
    public static final int TRAIL_LIFETIME = 14;

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(ChronosEchoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(ChronosEchoEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public ChronosEchoEntity(EntityType<? extends ChronosEchoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static ChronosEchoEntity create(Level level, ServerPlayer owner, int variant) {
        ChronosEchoEntity echo = new ChronosEchoEntity(ModEntities.ECHO.get(), level);
        echo.setVariant(variant);
        echo.setOwnerId(owner.getUUID());
        echo.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        return echo;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(VARIANT, BODY);
        builder.define(OWNER, Optional.empty());
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    public Optional<UUID> getOwnerId() {
        return this.entityData.get(OWNER);
    }

    public void setOwnerId(UUID id) {
        this.entityData.set(OWNER, Optional.of(id));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (getVariant() == TRAIL) {
            if (this.tickCount > TRAIL_LIFETIME) {
                this.discard();
            }
            return;
        }

        Optional<UUID> ownerId = getOwnerId();
        ServerPlayer owner = ownerId.isPresent() && this.level().getServer() != null
                ? this.level().getServer().getPlayerList().getPlayer(ownerId.get())
                : null;
        if (owner == null || owner.isRemoved()) {
            this.discard();
            return;
        }
        ChronosData data = ChronosLogic.data(owner);
        if (!this.getUUID().equals(data.ghostBody)) {
            this.discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && getVariant() == BODY && !this.isRemoved()) {
            Optional<UUID> ownerId = getOwnerId();
            if (ownerId.isPresent() && this.level().getServer() != null) {
                ServerPlayer owner = this.level().getServer().getPlayerList().getPlayer(ownerId.get());
                if (owner != null) {
                    AbilityManager.onBodyHit(owner);
                    float capped = Math.min(amount, AbilityManager.BODY_DAMAGE_CAP);
                    if (capped > 0f && !owner.isCreative() && !owner.isSpectator()) {
                        owner.hurt(source, capped);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(2.0);
    }

    @Override
    public boolean isPickable() {
        return getVariant() == BODY && !this.isRemoved();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
