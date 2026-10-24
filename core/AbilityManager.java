package dev.nullmod.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dev.nullmod.NullConfig;
import dev.nullmod.entity.ChronosEchoEntity;
import dev.nullmod.CaptainEvents;
import net.minecraft.world.entity.monster.Pillager;
import dev.nullmod.net.AbilityPayload;
import dev.nullmod.net.SpeechPayload;
import dev.nullmod.net.ZonesPayload;
import dev.nullmod.registry.ModEffects;
import dev.nullmod.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public final class AbilityManager {
    public static final int BULLET_TICKS_BASE = 100;
    public static final int GHOST_MAX_TICKS = 160;
    public static final int STOP_MAX_TICKS = 100;
    public static final int HUNTER_FREE_TICKS = 200;
    public static final float BODY_DAMAGE_CAP = 6.0f;
    public static final int REWIND_TICKS = 20;
    public static final float DRAGON_DAMAGE = 10.0f;
    public static final double DRAGON_HIT_RADIUS = 1.6;
    public static final int CLASH_GLITCH_TICKS = 10;
    public static final int IMPACT_TICKS = 4;
    public static final int MAX_TRAIL = 700;

    public static final int K_NONE = 0;
    public static final int K_BASE = 1;
    public static final int K_GHOST = 3;
    public static final int K_HUNTER = 4;
    public static final int K_STOP = 5;

    private static final float SLOWMO_VOLUME = 0.2f;

    private static final List<ServerPlayer> ZONE_OWNERS = new ArrayList<>();
    private record MobZone(LivingEntity source, long end) {}
    private static final List<MobZone> MOB_ZONES = new ArrayList<>();
    private static Set<ServerLevel> sentLevels = new HashSet<>();
    private static boolean zonesDirty;

    private AbilityManager() {}

    public static int rankOfKind(int kind) {
        return switch (kind) {
            case K_STOP -> 3;
            case K_GHOST, K_HUNTER -> 2;
            case K_BASE -> 1;
            default -> 0;
        };
    }

    public static int slowFactor(int kind) {
        return switch (kind) {
            case K_BASE, K_HUNTER -> 4;
            case K_GHOST -> 5;
            default -> 1;
        };
    }

    public static boolean zoneSkips(int kind, long gameTime) {
        if (kind == K_STOP) return true;
        int factor = slowFactor(kind);
        return factor > 1 && gameTime % factor != 0;
    }

    public static int kindOf(ChronosData d, long now) {
        if (d.timeStopActive(now)) return K_STOP;
        if (d.hunterOn) return K_HUNTER;
        if (d.ghostActive()) return K_GHOST;
        if (d.bulletActive(now)) return K_BASE;
        return K_NONE;
    }

    public static void handle(ServerPlayer player, AbilityPayload payload) {
        ChronosData d = ChronosLogic.data(player);
        long now = player.serverLevel().getGameTime();
        switch (payload.action()) {
            case AbilityPayload.TAP -> {
                if (d.tier == ChronosTier.HEADHUNTER.id) hunterToggle(player, d, now);
                else tap(player, d, now);
            }
            case AbilityPayload.GHOST_START -> {
                if (d.ghostActive()) {
                    endGhost(player, d, End.REWIND);
                    ChronosLogic.syncNow(player);
                } else {
                    ghostStart(player, d, now);
                }
            }
            case AbilityPayload.GHOST_END -> {
                if (d.ghostActive()) {
                    endGhost(player, d, End.REWIND);
                    ChronosLogic.syncNow(player);
                }
            }
            case AbilityPayload.DRAGON_START -> dragonStart(player, d, now);
            case AbilityPayload.DRAGON_FIRE -> dragonFire(player, d, now, payload.range());
            case AbilityPayload.DRAGON_CANCEL -> {
                if (d.timeStopActive(now)) {
                    endTimeStop(player, d, now, true);
                    ChronosLogic.syncNow(player);
                }
            }
            default -> { }
        }
    }

    private static boolean dominated(ServerPlayer player, int rank, long now) {
        if (!NullConfig.AFFECT_PLAYERS.get()) return false;
        double range = NullConfig.EFFECT_RANGE.get();
        for (ServerPlayer owner : ZONE_OWNERS) {
            if (owner == player || owner.level() != player.level()) continue;
            if (rankOfKind(kindOf(ChronosLogic.data(owner), now)) > rank
                    && owner.distanceToSqr(player) <= range * range) {
                return true;
            }
        }
        return false;
    }

    private static boolean blocked(ServerPlayer player, ChronosData d, int rank, long now) {
        if (!dominated(player, rank, now)) return false;
        d.clashGlitchUntil = now + CLASH_GLITCH_TICKS;
        ChronosLogic.syncNow(player);
        return true;
    }

    private static void tap(ServerPlayer player, ChronosData d, long now) {
        if (d.bulletActive(now) && !d.hunterOn) {
            endBullet(player, d);
            ChronosLogic.syncNow(player);
            return;
        }
        ChronosTier tier = d.tierEnum();
        if (tier != ChronosTier.BASE || !ChronosLogic.canUseAbilities(d, now)) {
            return;
        }
        if (now < d.bulletCdUntil || blocked(player, d, tier.rank, now)) {
            return;
        }
        int ticks = BULLET_TICKS_BASE;
        d.bulletEnd = now + ticks;
        d.bulletStart = now;
        d.bulletCdUntil = now + ChronosLogic.cooldown(player.serverLevel(), NullConfig.BULLET_COOLDOWN.get());
        player.addEffect(new MobEffectInstance(ModEffects.BULLET_TIME, ticks, 0, false, false, true));
        playCue(player, ModSounds.SLOWMO_1.get());
        ChronosLogic.charge(player, d, ChronosLogic.COST_BULLET);
        ChronosLogic.syncNow(player);
    }

    private static void endBullet(ServerPlayer player, ChronosData d) {
        d.bulletEnd = 0;
        d.trail.clear();
        player.removeEffect(ModEffects.BULLET_TIME);
    }

    private static void hunterToggle(ServerPlayer player, ChronosData d, long now) {
        if (d.hunterOn) {
            endHunter(player, d, now, false);
            ChronosLogic.syncNow(player);
            return;
        }
        if (d.tier != ChronosTier.HEADHUNTER.id || !ChronosLogic.canUseAbilities(d, now)
                || now < d.bulletCdUntil || blocked(player, d, 2, now)) {
            return;
        }
        d.hunterOn = true;
        d.bulletStart = now;
        resetTrail(player, d);
        player.addEffect(new MobEffectInstance(ModEffects.BULLET_TIME, 60, 0, false, false, true));
        playCue(player, ModSounds.SLOWMO_1.get());
        ChronosLogic.charge(player, d, ChronosLogic.COST_HUNTER_START);
        ChronosLogic.syncNow(player);
    }

    private static void endHunter(ServerPlayer player, ChronosData d, long now, boolean rewind) {
        d.hunterOn = false;
        d.bulletCdUntil = now + ChronosLogic.cooldown(player.serverLevel(), NullConfig.HUNTER_COOLDOWN.get());
        player.removeEffect(ModEffects.BULLET_TIME);
        if (rewind) startRewind(player, d);
        else d.trail.clear();
    }

    private static void ghostStart(ServerPlayer player, ChronosData d, long now) {
        if (d.tier != ChronosTier.ZERO.id || d.ghostActive() || !ChronosLogic.canUseAbilities(d, now)) {
            return;
        }
        if (now < d.holdCdUntil || d.bulletActive(now) || blocked(player, d, 2, now)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ChronosEchoEntity body = ChronosEchoEntity.create(level, player, ChronosEchoEntity.BODY);
        level.addFreshEntity(body);
        d.ghostBody = body.getUUID();
        d.ghostEnd = now + GHOST_MAX_TICKS;
        resetTrail(player, d);
        player.addEffect(new MobEffectInstance(ModEffects.GHOST, GHOST_MAX_TICKS + 10, 0, false, false, true));
        playCue(player, ModSounds.SLOWMO_2.get());
        ChronosLogic.charge(player, d, ChronosLogic.COST_GHOST_START);
        ChronosLogic.syncNow(player);
    }

    public enum End { QUIET, INSTANT, REWIND }

    private static void endGhost(ServerPlayer player, ChronosData d, End mode) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Entity body = d.ghostBody == null ? null : level.getEntity(d.ghostBody);
        ChronosData.TrailPoint home = !d.trail.isEmpty() ? d.trail.get(0)
                : (body != null ? new ChronosData.TrailPoint(body.getX(), body.getY(), body.getZ(), player.getYRot(), player.getXRot()) : null);
        if (body != null) {
            body.discard();
        }
        d.ghostBody = null;
        d.ghostEnd = 0;
        d.holdCdUntil = now + ChronosLogic.cooldown(level, NullConfig.HOLD_COOLDOWN.get());
        player.removeEffect(ModEffects.GHOST);

        if (mode == End.INSTANT && home != null) {
            snapTo(player, home);
            d.trail.clear();
        } else if (mode == End.REWIND) {
            startRewind(player, d);
        } else {
            d.trail.clear();
        }
    }

    private static void snapTo(ServerPlayer player, ChronosData.TrailPoint p) {
        player.connection.teleport(p.x(), p.y(), p.z(), p.yaw(), p.pitch());
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static ChronosData.TrailPoint pointOf(ServerPlayer player) {
        return new ChronosData.TrailPoint(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    private static void resetTrail(ServerPlayer player, ChronosData d) {
        d.trail.clear();
        d.trail.add(pointOf(player));
    }

    private static void record(ServerPlayer player, ChronosData d) {
        if (d.trail.size() >= MAX_TRAIL) {
            d.trail.remove(1);
        }
        d.trail.add(pointOf(player));
    }

    private static void startRewind(ServerPlayer player, ChronosData d) {
        if (d.trail.size() < 2) {
            d.trail.clear();
            return;
        }
        d.trail.add(pointOf(player));
        d.rewinding = true;
        d.rewindTick = 0;
        d.rewindFrom = d.trail.size() - 1;
        player.addEffect(new MobEffectInstance(ModEffects.REWIND, REWIND_TICKS + 10, 0, false, false, true));
    }

    private static ChronosData.TrailPoint sample(ChronosData d, float index) {
        int max = d.trail.size() - 1;
        float f = Mth.clamp(index, 0.0f, (float) max);
        int i0 = (int) Math.floor(f);
        int i1 = Math.min(max, i0 + 1);
        float t = f - i0;
        ChronosData.TrailPoint a = d.trail.get(i0);
        ChronosData.TrailPoint b = d.trail.get(i1);
        return new ChronosData.TrailPoint(
                Mth.lerp(t, a.x(), b.x()), Mth.lerp(t, a.y(), b.y()), Mth.lerp(t, a.z(), b.z()),
                a.yaw() + Mth.wrapDegrees(b.yaw() - a.yaw()) * t,
                Mth.lerp(t, a.pitch(), b.pitch()));
    }

    private static void tickRewind(ServerPlayer player, ChronosData d) {
        d.rewindTick++;
        float p = Math.min(1.0f, d.rewindTick / (float) REWIND_TICKS);
        float eased = p * p * (3.0f - 2.0f * p);
        boolean done = d.rewindTick >= REWIND_TICKS;
        ChronosData.TrailPoint at = done ? d.trail.get(0) : sample(d, d.rewindFrom * (1.0f - eased));
        snapTo(player, at);
        if (done) {
            d.trail.clear();
            d.rewinding = false;
            player.removeEffect(ModEffects.REWIND);
            ChronosLogic.syncNow(player);
        }
    }

    public static boolean isRewinding(ServerPlayer player) {
        return ChronosLogic.data(player).rewinding;
    }

    private static void abort(ServerPlayer player, ChronosData d, long now) {
        if (d.ghostActive()) {
            endGhost(player, d, End.REWIND);
        } else if (d.hunterOn) {
            endHunter(player, d, now, true);
        } else if (d.bulletActive(now)) {
            d.bulletEnd = 0;
            player.removeEffect(ModEffects.BULLET_TIME);
            startRewind(player, d);
        }
    }

    public static void onPlayerHurt(ServerPlayer player) {
        ChronosData d = ChronosLogic.data(player);
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (d.rewinding) return;

        UUID partnerId = d.clashWith;
        boolean ghost = d.ghostActive();
        boolean clashing = partnerId != null && kindOf(d, now) != K_NONE;
        if (!ghost && !clashing) return;

        abort(player, d, now);
        if (ghost || d.tier == ChronosTier.ZERO.id) {
            ChronosLogic.speak(player, SpeechPayload.NO_WORK, 0);
        }
        d.clashWith = null;

        if (clashing) {
            ServerPlayer partner = level.getServer().getPlayerList().getPlayer(partnerId);
            if (partner != null) {
                ChronosData pd = ChronosLogic.data(partner);
                if (!pd.rewinding && kindOf(pd, now) != K_NONE) {
                    abort(partner, pd, now);
                    if (pd.tier == ChronosTier.ZERO.id) {
                        ChronosLogic.speak(partner, SpeechPayload.NO_WORK, 0);
                    }
                }
                pd.clashWith = null;
                ChronosLogic.syncNow(partner);
            }
        }
        zonesDirty = true;
        ChronosLogic.syncNow(player);
    }

    public static void onBodyHit(ServerPlayer owner) {
        ChronosData d = ChronosLogic.data(owner);
        if (!d.ghostActive() || d.rewinding) return;
        long now = owner.serverLevel().getGameTime();
        UUID partnerId = d.clashWith;
        endGhost(owner, d, End.INSTANT);
        d.clashWith = null;
        if (partnerId != null) {
            ServerPlayer partner = owner.serverLevel().getServer().getPlayerList().getPlayer(partnerId);
            if (partner != null) {
                ChronosData pd = ChronosLogic.data(partner);
                if (!pd.rewinding && kindOf(pd, now) != K_NONE) {
                    abort(partner, pd, now);
                    if (pd.tier == ChronosTier.ZERO.id) {
                        ChronosLogic.speak(partner, SpeechPayload.NO_WORK, 0);
                    }
                }
                pd.clashWith = null;
                ChronosLogic.syncNow(partner);
            }
        }
        zonesDirty = true;
        ChronosLogic.syncNow(owner);
    }

    private static void dragonStart(ServerPlayer player, ChronosData d, long now) {
        if (d.tier != ChronosTier.DRAGON.id || !ChronosLogic.canUseAbilities(d, now)) {
            return;
        }
        if (d.timeStopActive(now) || now < d.dragonCdUntil) {
            return;
        }
        d.timeStopEnd = now + STOP_MAX_TICKS;
        player.addEffect(new MobEffectInstance(ModEffects.TIME_STOP, STOP_MAX_TICKS + 10, 0, false, false, true));
        playCue(player, ModSounds.SLOWMO_3.get());
        ChronosLogic.syncNow(player);
    }

    private static void endTimeStop(ServerPlayer player, ChronosData d, long now, boolean cancelled) {
        d.timeStopEnd = 0;
        player.removeEffect(ModEffects.TIME_STOP);
        long cooldown = ChronosLogic.cooldown(player.serverLevel(), NullConfig.DRAGON_COOLDOWN.get());
        d.dragonCdUntil = now + (cancelled ? cooldown / 4 : cooldown);
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lengthSq = ab.lengthSqr();
        if (lengthSq < 1.0e-6) return p.distanceTo(a);
        double t = Mth.clamp(p.subtract(a).dot(ab) / lengthSq, 0.0, 1.0);
        return p.distanceTo(a.add(ab.scale(t)));
    }


    private static void dragonFire(ServerPlayer player, ChronosData d, long now, float requested) {
        if (!d.timeStopActive(now)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        float range = Mth.clamp(requested, 1.0f, NullConfig.DRAGON_MAX_RANGE.get());

        Vec3 travelled = AimMath.travel(player, range);
        Vec3 start = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Vec3 end = start.add(travelled);

        AABB sweep = new AABB(start, end).inflate(DRAGON_HIT_RADIUS + 2.0);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, sweep, e ->
                e != player && e.isAlive() && !e.isSpectator()
                        && (!(e instanceof Player) || NullConfig.DRAGON_PVP.get()));
        double travelLength = travelled.length();
        Vec3 slashDir = travelLength > 0.01 ? travelled.scale(1.0 / travelLength) : player.getLookAngle().normalize();

        int hits = 0;
        for (LivingEntity victim : victims) {
            Vec3 center = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
            double reach = DRAGON_HIT_RADIUS + victim.getBbWidth() * 0.5 + victim.getBbHeight() * 0.25;
            if (distanceToSegment(center, start, end) <= reach) {
                hits++;
                victim.invulnerableTime = 0;
                victim.hurt(player.damageSources().playerAttack(player), DRAGON_DAMAGE);
                victim.knockback(0.8, -slashDir.x, -slashDir.z);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 6, false, false, false));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 14, 0.3, 0.4, 0.3, 0.2);
            }
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                hits > 0 ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0f, hits > 0 ? 1.0f : 1.3f);
        double reachSq = NullConfig.EFFECT_RANGE.get() * NullConfig.EFFECT_RANGE.get();
        for (ServerPlayer witness : level.players()) {
            if (witness == player || witness.distanceToSqr(player) > reachSq) continue;
            ChronosLogic.data(witness).impactUntil = now + IMPACT_TICKS;
            ChronosLogic.syncNow(witness);
        }

        double length = travelled.length();
        if (length > 0.01) {
            Vec3 dir = travelled.scale(1.0 / length);
            DustParticleOptions cyan = new DustParticleOptions(new Vector3f(0.0f, 0.9f, 1.0f), 1.6f);
            DustParticleOptions magenta = new DustParticleOptions(new Vector3f(1.0f, 0.17f, 0.84f), 1.6f);
            int i = 0;
            for (double t = 0; t < length; t += 0.5, i++) {
                Vec3 p = start.add(dir.scale(t));
                level.sendParticles((i & 1) == 0 ? cyan : magenta, p.x, p.y, p.z, 2, 0.12, 0.2, 0.12, 0);
            }
            double gap = Math.max(1.5, length / 10.0);
            for (double t = 0; t < length; t += gap) {
                Vec3 feet = player.position().add(dir.scale(t));
                ChronosEchoEntity echo = ChronosEchoEntity.create(level, player, ChronosEchoEntity.TRAIL);
                echo.moveTo(feet.x, feet.y, feet.z, player.getYRot(), player.getXRot());
                level.addFreshEntity(echo);
            }
        }

        player.connection.teleport(player.getX() + travelled.x, player.getY() + travelled.y, player.getZ() + travelled.z,
                player.getYRot(), player.getXRot());
        player.resetFallDistance();

        endTimeStop(player, d, now, false);
        playCue(player, ModSounds.SLOWMO_3.get());
        ChronosLogic.charge(player, d, ChronosLogic.COST_DRAGON);
        ChronosLogic.syncNow(player);
    }

    public static void tick(ServerPlayer player, ChronosData d, long now) {
        boolean changed = false;

        if (!d.hunterOn && d.bulletEnd != 0 && now >= d.bulletEnd) {
            endBullet(player, d);
            changed = true;
        }
        if (d.hunterOn) {
            if (now - d.bulletStart > HUNTER_FREE_TICKS) {
                float ramp = Math.min(10.0f, 1.0f + (now - d.bulletStart - HUNTER_FREE_TICKS) / 60.0f);
                ChronosLogic.drainTick(player, d, ChronosLogic.COST_HUNTER_PER_TICK * ramp);
            }
            if (d.hunterOn && now % 20 == 0) {
                player.addEffect(new MobEffectInstance(ModEffects.BULLET_TIME, 60, 0, false, false, true));
            }
        }
        if (d.timeStopEnd != 0 && now >= d.timeStopEnd) {
            endTimeStop(player, d, now, true);
            changed = true;
        }
        if (d.ghostActive()) {
            if (now >= d.ghostEnd) {
                endGhost(player, d, End.REWIND);
                changed = true;
            } else {
                ChronosLogic.drainTick(player, d, ChronosLogic.COST_GHOST_PER_TICK);
                if (d.ghostActive() && now % 2 == 0 && player.distanceToSqr(player.xo, player.yo, player.zo) > 0.0004) {
                    ServerLevel level = player.serverLevel();
                    level.addFreshEntity(ChronosEchoEntity.create(level, player, ChronosEchoEntity.TRAIL));
                }
            }
        }
        if (d.rewinding) {
            tickRewind(player, d);
        } else if (d.ghostActive() || d.hunterOn || (d.tier == ChronosTier.ZERO.id && d.bulletActive(now))) {
            record(player, d);
        }

        int kind = kindOf(d, now);
        if (kind != K_NONE) {
            if (!ZONE_OWNERS.contains(player)) {
                ZONE_OWNERS.add(player);
            }
            if (now % 5 == 0 && NullConfig.AFFECT_PLAYERS.get()) {
                slowNearbyPlayers(player, kind, now);
            }
        } else {
            ZONE_OWNERS.remove(player);
            d.clashWith = null;
        }
        if (kind != d.lastKind) {
            d.lastKind = kind;
            zonesDirty = true;
        }

        if (changed) {
            ChronosLogic.syncNow(player);
        }
    }

    private static void slowNearbyPlayers(ServerPlayer owner, int kind, long now) {
        double range = NullConfig.EFFECT_RANGE.get();
        int rank = rankOfKind(kind);
        boolean stop = kind == K_STOP;
        for (ServerPlayer other : owner.serverLevel().players()) {
            if (other == owner || other.distanceToSqr(owner) > range * range) continue;
            if (rankOfKind(kindOf(ChronosLogic.data(other), now)) >= rank) continue;
            other.addEffect(new MobEffectInstance(stop ? ModEffects.TIME_STOPPED : ModEffects.TEMPORAL_DRAG,
                    12, 0, false, false, true));
        }
    }

    public static void startMobZone(LivingEntity source, int ticks) {
        MOB_ZONES.add(new MobZone(source, source.level().getGameTime() + ticks));
        zonesDirty = true;
        if (source.level() instanceof ServerLevel level) {
            slowAround(level, source, source.getX(), source.getY(), source.getZ());
        }
    }

    private static void slowAround(ServerLevel level, LivingEntity source, double x, double y, double z) {
        double range = NullConfig.EFFECT_RANGE.get();
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(x, y, z) > range * range) continue;
            if (rankOfKind(kindOf(ChronosLogic.data(other), level.getGameTime())) >= 1) continue;
            other.addEffect(new MobEffectInstance(ModEffects.TEMPORAL_DRAG, 12, 0, false, false, true));
        }
    }

    private static void tickMobZones() {
        if (MOB_ZONES.isEmpty()) return;
        boolean removed = MOB_ZONES.removeIf(z -> {
            boolean gone = z.source().isRemoved() || !z.source().isAlive()
                    || z.source().level().getGameTime() >= z.end();
            return gone;
        });
        if (removed) zonesDirty = true;
        for (MobZone z : MOB_ZONES) {
            if (z.source().level() instanceof ServerLevel level && level.getGameTime() % 5 == 0) {
                slowAround(level, z.source(), z.source().getX(), z.source().getY(), z.source().getZ());
            }
        }
    }

    public static void resolve(MinecraftServer server) {
        tickMobZones();
        if (ZONE_OWNERS.size() >= 2 && NullConfig.AFFECT_PLAYERS.get()) {
            double range = NullConfig.EFFECT_RANGE.get();
            List<ServerPlayer> owners = new ArrayList<>(ZONE_OWNERS);
            for (ServerPlayer a : owners) {
                ChronosData da = ChronosLogic.data(a);
                da.clashWith = null;
            }
            for (ServerPlayer a : owners) {
                for (ServerPlayer b : owners) {
                    if (a == b || a.level() != b.level() || a.distanceToSqr(b) > range * range) continue;
                    ChronosData da = ChronosLogic.data(a);
                    ChronosData db = ChronosLogic.data(b);
                    long t = a.serverLevel().getGameTime();
                    int ka = kindOf(da, t);
                    int kb = kindOf(db, t);
                    if (ka == K_NONE || kb == K_NONE) continue;
                    int ra = rankOfKind(ka);
                    int rb = rankOfKind(kb);
                    if (ra > rb) {
                        suppress(b, db, t);
                    } else if (ra == rb && ra == 2 && (ka == K_HUNTER) != (kb == K_HUNTER)) {
                        da.clashWith = b.getUUID();
                        db.clashWith = a.getUUID();
                    }
                }
            }
        } else {
            for (ServerPlayer a : ZONE_OWNERS) {
                ChronosLogic.data(a).clashWith = null;
            }
        }
        captainClashes();
        broadcastZones(server);
    }

    private static final Set<UUID> CAPTAIN_CLASHING = new HashSet<>();

    private static void captainClashes() {
        if (ZONE_OWNERS.isEmpty() && CAPTAIN_CLASHING.isEmpty()) return;
        double range = NullConfig.EFFECT_RANGE.get();
        Set<UUID> now = new HashSet<>();
        for (ServerPlayer owner : ZONE_OWNERS) {
            ChronosData d = ChronosLogic.data(owner);
            long t = owner.serverLevel().getGameTime();
            int kind = kindOf(d, t);
            if (kind == K_NONE || kind == K_STOP || d.clashWith != null) continue;
            List<Pillager> near = owner.serverLevel().getEntitiesOfClass(Pillager.class,
                    owner.getBoundingBox().inflate(range), c -> c.isAlive() && CaptainEvents.isChronos(c) && c.distanceToSqr(owner) <= range * range);
            if (near.isEmpty()) continue;
            d.clashWith = near.get(0).getUUID();
            now.add(owner.getUUID());
            if (!CAPTAIN_CLASHING.contains(owner.getUUID())) {
                ChronosLogic.speak(owner, SpeechPayload.CAPTAIN, 0);
                zonesDirty = true;
            }
        }
        if (!now.equals(CAPTAIN_CLASHING)) zonesDirty = true;
        CAPTAIN_CLASHING.clear();
        CAPTAIN_CLASHING.addAll(now);
    }

    private static void suppress(ServerPlayer player, ChronosData d, long now) {
        if (d.ghostActive()) {
            endGhost(player, d, End.REWIND);
        } else if (d.hunterOn) {
            endHunter(player, d, now, false);
        } else if (d.timeStopActive(now)) {
            endTimeStop(player, d, now, true);
        } else if (d.bulletActive(now)) {
            endBullet(player, d);
        }
        d.clashGlitchUntil = now + CLASH_GLITCH_TICKS;
        zonesDirty = true;
        ChronosLogic.syncNow(player);
    }

    private static void broadcastZones(MinecraftServer server) {
        if (!zonesDirty && ((ZONE_OWNERS.isEmpty() && MOB_ZONES.isEmpty()) || server.getTickCount() % 5 != 0)) {
            return;
        }
        zonesDirty = false;
        Map<ServerLevel, List<ZonesPayload.Entry>> byLevel = new HashMap<>();
        for (ServerPlayer owner : ZONE_OWNERS) {
            ChronosData d = ChronosLogic.data(owner);
            ServerLevel level = owner.serverLevel();
            int kind = kindOf(d, level.getGameTime());
            if (kind == K_NONE) continue;
            byLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(new ZonesPayload.Entry(
                    owner.getId(), (float) owner.getX(), (float) owner.getY(), (float) owner.getZ(),
                    kind, d.clashWith != null));
        }
        for (MobZone z : MOB_ZONES) {
            if (!(z.source().level() instanceof ServerLevel level)) continue;
            byLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(new ZonesPayload.Entry(
                    z.source().getId(), (float) z.source().getX(), (float) z.source().getY(),
                    (float) z.source().getZ(), K_BASE, false));
        }
        Set<ServerLevel> touched = new HashSet<>(byLevel.keySet());
        touched.addAll(sentLevels);
        float range = NullConfig.EFFECT_RANGE.get();
        for (ServerLevel level : touched) {
            List<ZonesPayload.Entry> entries = byLevel.getOrDefault(level, List.of());
            PacketDistributor.sendToPlayersInDimension(level, new ZonesPayload(range, entries));
        }
        sentLevels = new HashSet<>(byLevel.keySet());
    }

    public static boolean shouldSkipTick(Entity entity) {
        if ((ZONE_OWNERS.isEmpty() && MOB_ZONES.isEmpty()) || entity.level().isClientSide
                || entity instanceof Player || entity instanceof ChronosEchoEntity) {
            return false;
        }
        long gameTime = entity.level().getGameTime();
        double range = NullConfig.EFFECT_RANGE.get();
        int factor = 1;
        for (MobZone z : MOB_ZONES) {
            if (z.source() == entity || z.source().level() != entity.level()) continue;
            if (z.source().distanceToSqr(entity) <= range * range) {
                factor = Math.max(factor, slowFactor(K_BASE));
            }
        }
        for (ServerPlayer owner : ZONE_OWNERS) {
            if (owner.level() != entity.level() || owner.distanceToSqr(entity) > range * range) continue;
            int kind = kindOf(ChronosLogic.data(owner), gameTime);
            if (kind == K_STOP) {
                return true;
            }
            factor = Math.max(factor, slowFactor(kind));
        }
        return factor > 1 && (gameTime + entity.getId()) % factor != 0;
    }

    public static void endAll(ServerPlayer player, ChronosData d, boolean rewindGhost) {
        long now = player.serverLevel().getGameTime();
        if (d.ghostActive()) endGhost(player, d, rewindGhost ? End.REWIND : End.QUIET);
        if (d.hunterOn) endHunter(player, d, now, false);
        if (d.bulletEnd != 0) endBullet(player, d);
        if (d.timeStopEnd != 0) endTimeStop(player, d, now, true);
        if (!rewindGhost || !d.rewinding) {
            if (d.rewinding) player.removeEffect(ModEffects.REWIND);
            d.rewinding = false;
            d.trail.clear();
        }
        d.clashWith = null;
        ZONE_OWNERS.remove(player);
        zonesDirty = true;
    }

    public static void forget(ServerPlayer player) {
        ZONE_OWNERS.remove(player);
        zonesDirty = true;
    }

    public static void clearAll() {
        ZONE_OWNERS.clear();
        MOB_ZONES.clear();
        sentLevels = new HashSet<>();
    }

    private static void playCue(ServerPlayer player, SoundEvent sound) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, SLOWMO_VOLUME, 1.0f);
    }
}
