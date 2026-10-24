package dev.nullmod.core;

import dev.nullmod.NullConfig;
import dev.nullmod.net.SpeechPayload;
import dev.nullmod.net.SyncPayload;
import dev.nullmod.registry.ModAttachments;
import dev.nullmod.registry.ModEffects;
import dev.nullmod.registry.ModGameRules;
import dev.nullmod.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ChronosLogic {

    public static final float COST_BULLET = 0.04f;
    public static final float COST_GHOST_START = 0.04f;
    public static final float COST_GHOST_PER_TICK = 0.0003f;
    public static final float COST_HUNTER_START = 0.03f;
    public static final float COST_HUNTER_PER_TICK = 0.0007f;
    public static final float COST_DRAGON = 0.12f;

    public static final float COST_AFFINITY_MAX = 0.10f;
    public static final float COST_CAP = 0.25f;

    public static final float WITHDRAWAL_START = 0.30f;
    public static final float WITHDRAWAL_DEEP = 0.15f;
    private static final int EMPTY_TO_STAGE_4 = 6000;

    private static final int[] GLITCH_LEN = {0, 20, 30, 45, 60};
    private static final float[] GLITCH_CHANCE = {0f, 0.02f, 0.05f, 0.10f, 0.18f};

    private static final int CUTSCENE_TEXT_GAP = 70;
    private static final int CUTSCENE_SHADER_LEN = 200;

    private ChronosLogic() {}

    public static ChronosData data(Player player) {
        return player.getData(ModAttachments.CHRONOS.get());
    }

    public static boolean withdrawalOn(ServerLevel level) {
        return NullConfig.WITHDRAWAL.get() && level.getGameRules().getBoolean(ModGameRules.WITHDRAWAL);
    }

    public static int rollTier(RandomSource random) {
        int roll = random.nextInt(100);
        int dragon = NullConfig.DRAGON_CHANCE.get();
        int hunter = NullConfig.HUNTER_CHANCE.get();
        int gov = NullConfig.GOV_CHANCE.get();
        if (roll < dragon) return ChronosTier.DRAGON.id;
        if (roll < dragon + hunter) return ChronosTier.HEADHUNTER.id;
        if (roll < dragon + hunter + gov) return ChronosTier.ZERO.id;
        return ChronosTier.BASE.id;
    }

    public static boolean tryDose(ServerPlayer player, int forcedTier) {
        if (lockedOut(player, true)) {
            return false;
        }
        ChronosData cur = data(player);
        if (forcedTier <= 0 && cur.tier > 0) {
            refill(player, cur);
        } else {
            applyDose(player, forcedTier > 0 ? forcedTier : rollTier(player.getRandom()));
        }
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.INJECTION.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        return true;
    }

    public static void refill(ServerPlayer player, ChronosData d) {
        long now = player.serverLevel().getGameTime();
        AbilityManager.endAll(player, d, false);
        d.blood = 1.0f;
        d.lastDoseTick = now;
        resetWithdrawal(player, d);
        sync(player, d, now, 0);
    }

    public static void applyDose(ServerPlayer player, int tier) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        ChronosData d = data(player);
        AbilityManager.endAll(player, d, false);
        d.tier = tier;
        d.blood = 1.0f;
        d.dependency = Math.max(d.dependency, ChronosTier.rankOf(tier));
        d.affinity = 1 + player.getRandom().nextInt(100);
        d.lastDoseTick = now;
        resetWithdrawal(player, d);
        sync(player, d, now, 0);
    }

    public static void removeChronos(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        ChronosData d = data(player);
        AbilityManager.endAll(player, d, false);
        d.tier = 0;
        d.blood = 0f;
        d.dependency = 0;
        d.affinity = -1;
        resetWithdrawal(player, d);
        sync(player, d, now, 0);
    }

    private static void resetWithdrawal(ServerPlayer player, ChronosData d) {
        d.withdrawalTicks = 0;
        d.glitchEnd = 0;
        d.cueDone = false;
        d.cutscenePhase = 0;
        d.forcedUntil = 0;
        d.lastStage = 0;
        clearWithdrawalEffects(player);
    }

    public static boolean lockedOut(ServerPlayer player, boolean speakWhy) {
        long now = player.serverLevel().getGameTime();
        ChronosData d = data(player);
        if (now >= d.cleanseUntil) {
            return false;
        }
        if (speakWhy) {
            speak(player, SpeechPayload.CLEANSE_BLOCKED, minutesLeft(d.cleanseUntil - now));
        }
        return true;
    }

    public static void cleanse(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        removeChronos(player);
        ChronosData d = data(player);
        d.cleanseUntil = now + (long) (NullConfig.CLEANSE_DAYS.get() * 24000.0);
        sync(player, d, now, 0);
    }

    public static void onDrink(ServerPlayer player, String potionPath) {
        switch (potionPath) {
            case "chronos" -> tryDose(player, 0);
            case "cleansing" -> cleanse(player);
            case "chronos_base", "cleansing_base" ->
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            default -> { }
        }
    }

    private static int minutesLeft(long ticksLeft) {
        return Math.max(1, (int) Math.ceil(ticksLeft / 20.0 / 60.0));
    }

    public static float affinityExtra(int affinity) {
        int a = affinity < 0 ? 50 : affinity;
        return (1.0f - a / 100.0f) * COST_AFFINITY_MAX;
    }

    public static float useCost(float base, int affinity) {
        return Math.min(COST_CAP, base + affinityExtra(affinity));
    }

    public static void charge(ServerPlayer player, ChronosData d, float base) {
        spend(player, d, useCost(base, d.affinity));
    }

    public static void drainTick(ServerPlayer player, ChronosData d, float base) {
        int a = d.affinity < 0 ? 50 : d.affinity;
        spend(player, d, base * (2.0f - a / 100.0f));
    }

    private static void spend(ServerPlayer player, ChronosData d, float amount) {
        if (d.blood <= 0f) return;
        if (!player.serverLevel().getGameRules().getBoolean(ModGameRules.DEPLETION)) return;
        d.blood = Math.max(0f, d.blood - amount);
        if (d.blood <= 0f) {
            onEmpty(player, d, true);
        }
    }

    public static long cooldown(ServerLevel level, int seconds) {
        return level.getGameRules().getBoolean(ModGameRules.NO_COOLDOWNS) ? 0L : seconds * 20L;
    }

    private static void onEmpty(ServerPlayer player, ChronosData d, boolean fromAbility) {
        ChronosTier tier = d.tierEnum();
        AbilityManager.endAll(player, d, true);

        if (fromAbility && tier == ChronosTier.DRAGON && withdrawalOn(player.serverLevel())) {
            d.withdrawalTicks = EMPTY_TO_STAGE_4;
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0));
            speak(player, SpeechPayload.HIGH_2, 0);
        }
    }

    public static boolean canUseAbilities(ChronosData d, long now) {
        return d.tier > 0 && d.blood > 0f && d.lastStage < 1 && !d.glitching(now)
                && !d.cutsceneActive(now) && !d.rewinding;
    }

    public static void tick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        ChronosData d = data(player);

        AbilityManager.tick(player, d, now);

        boolean inWithdrawal = withdrawalOn(level) && d.dependency > 0 && d.blood <= 0f;
        if (inWithdrawal) d.withdrawalTicks++;
        else if (d.withdrawalTicks != 0) d.withdrawalTicks = 0;

        int stage = stageOf(level, d);
        if (stage != d.lastStage) {
            onStageChange(player, d, now, stage);
        }
        if (stage > 0) {
            if (now % 20 == 0) applyWithdrawalEffects(player, stage);
            tickGlitchAndSpeech(player, d, now, stage);
        }
        tickCutscene(player, d, now, stage);
        syncIfChanged(player, d, now, stage);
    }

    public static int stageOf(ServerLevel level, ChronosData d) {
        if (!withdrawalOn(level) || d.dependency <= 0 || d.blood >= WITHDRAWAL_START) return 0;

        int stage;
        if (d.blood >= WITHDRAWAL_DEEP) stage = 1;
        else if (d.blood > 0f) stage = 2;
        else stage = d.withdrawalTicks >= EMPTY_TO_STAGE_4 ? 4 : 3;

        int cap = d.dependency >= 3 ? 4 : (d.dependency == 2 ? 3 : 2);
        return Math.min(stage, cap);
    }

    public static int visualStage(ChronosData d, int stage, long now) {
        return d.forcedVisual(now) ? Math.max(stage, 4) : stage;
    }

    private static void onStageChange(ServerPlayer player, ChronosData d, long now, int stage) {
        int previous = d.lastStage;
        d.lastStage = stage;
        if (stage == 0) {
            d.glitchEnd = 0;
            d.cueDone = false;
            clearWithdrawalEffects(player);
        } else {
            d.nextSpeech = now + 80;
            applyWithdrawalEffects(player, stage);
            if (previous < 1) {
                AbilityManager.endAll(player, d, true);
                if (!d.cueDone) {
                    d.cueDone = true;
                    startCutscene(player, d, now);
                }
            }
        }
        sync(player, d, now, stage);
    }

    public static void startCutscene(ServerPlayer player, ChronosData d, long now) {
        AbilityManager.endAll(player, d, true);
        d.cutscenePhase = 1;
        d.cutsceneStart = now;
        d.cutsceneShaderAt = now + CUTSCENE_TEXT_GAP;
        d.forcedUntil = d.cutsceneShaderAt + CUTSCENE_SHADER_LEN;
        speak(player, SpeechPayload.HIGH_1, 0);
        syncNow(player);
    }

    private static void tickCutscene(ServerPlayer player, ChronosData d, long now, int stage) {
        if (d.cutscenePhase == 0) return;
        if (d.cutscenePhase == 1 && now >= d.cutsceneStart + 40) {
            d.cutscenePhase = 2;
            speak(player, SpeechPayload.HIGH_2, 0);
        }
        if (d.cutscenePhase == 2 && now >= d.cutsceneShaderAt) {
            d.cutscenePhase = 3;
            d.glitchEnd = Math.max(d.glitchEnd, d.forcedUntil);
            syncNow(player);
        }
        if (now >= d.forcedUntil) {
            d.cutscenePhase = 0;
            d.forcedUntil = 0;
            syncNow(player);
        }
    }

    private static void applyWithdrawalEffects(ServerPlayer player, int stage) {
        player.addEffect(new MobEffectInstance(ModEffects.WITHDRAWAL, 100, stage - 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, stage >= 3 ? 1 : 0, false, false, true));
        if (stage >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, stage >= 4 ? 1 : 0, false, false, true));
        }
        if (stage >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 260, 0, false, false, true));
        }
    }

    private static void clearWithdrawalEffects(ServerPlayer player) {
        player.removeEffect(ModEffects.WITHDRAWAL);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.BLINDNESS);
    }

    private static void tickGlitchAndSpeech(ServerPlayer player, ChronosData d, long now, int stage) {
        RandomSource random = player.getRandom();

        if (!d.glitching(now) && now % 20 == 0 && random.nextFloat() < GLITCH_CHANCE[stage]) {
            int length = GLITCH_LEN[stage] + random.nextInt(GLITCH_LEN[stage] / 2 + 1);
            d.glitchEnd = now + length;

            if (stage >= 4) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, length + 10, 0, false, false, false));
            }
            sync(player, d, now, stage);
        }

        if (now >= d.nextSpeech && !d.cutsceneActive(now)) {
            if (stage >= 3) {
                speak(player, random.nextBoolean() ? SpeechPayload.HIGH_1 : SpeechPayload.HIGH_2, 0);
                d.nextSpeech = now + 400 + random.nextInt(500);
            } else {
                speak(player, random.nextBoolean() ? SpeechPayload.LOW_1 : SpeechPayload.LOW_2, 0);
                d.nextSpeech = now + 800 + random.nextInt(1200);
            }
        }
    }

    public static void speak(ServerPlayer player, int kind, int arg) {
        PacketDistributor.sendToPlayer(player, new SpeechPayload(kind, arg));
    }

    public static SyncPayload snapshot(ChronosData d, long now, int stage) {
        boolean forced = d.forcedVisual(now);
        int flags = (d.bulletActive(now) ? SyncPayload.FLAG_BULLET : 0)
                | (d.ghostActive() ? SyncPayload.FLAG_GHOST : 0)
                | (d.timeStopActive(now) ? SyncPayload.FLAG_STOP : 0)
                | (d.glitching(now) ? SyncPayload.FLAG_GLITCH : 0)
                | (forced ? SyncPayload.FLAG_FORCED : 0)
                | (d.rewinding ? SyncPayload.FLAG_REWIND : 0)
                | (now < d.clashGlitchUntil ? SyncPayload.FLAG_CLASH_GLITCH : 0)
                | (now < d.impactUntil ? SyncPayload.FLAG_IMPACT : 0);
        return new SyncPayload(d.blood, d.tier, d.dependency, d.affinity,
                (int) Math.max(0, d.cleanseUntil - now),
                (int) Math.max(0, d.bulletCdUntil - now),
                (int) Math.max(0, d.holdCdUntil - now),
                (int) Math.max(0, d.dragonCdUntil - now),
                visualStage(d, stage, now), flags, NullConfig.DRAGON_MAX_RANGE.get());
    }

    public static void sync(ServerPlayer player, ChronosData d, long now, int stage) {
        SyncPayload payload = snapshot(d, now, stage);
        d.lastSyncHash = hashOf(payload);
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void syncNow(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ChronosData d = data(player);
        sync(player, d, level.getGameTime(), stageOf(level, d));
    }

    private static void syncIfChanged(ServerPlayer player, ChronosData d, long now, int stage) {
        SyncPayload payload = snapshot(d, now, stage);
        int hash = hashOf(payload);
        if (hash != d.lastSyncHash || now % 100 == 0) {
            d.lastSyncHash = hash;
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static int hashOf(SyncPayload p) {
        int h = Math.round(p.blood() * 200f);
        h = h * 31 + p.tier();
        h = h * 31 + p.dependency();
        h = h * 31 + p.affinity();
        h = h * 31 + p.stage();
        h = h * 31 + p.flags();
        h = h * 31 + (p.cleanseLeft() > 0 ? 1 : 0);
        return h;
    }
}
