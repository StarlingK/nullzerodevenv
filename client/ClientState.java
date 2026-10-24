package dev.nullmod.client;

import dev.nullmod.core.ChronosLogic;
import dev.nullmod.core.ChronosTier;
import dev.nullmod.net.SpeechPayload;
import dev.nullmod.net.SyncPayload;
import net.minecraft.network.chat.Component;

public final class ClientState {
    public static float blood;
    public static int tier, dependency, affinity = -1, stage, flags;

    public static int cleanseLeft, bulletCd, holdCd, dragonCd;

    public static int speechKind = -1, speechArg, speechAge;
    public static final int SPEECH_LIFE = 90;

    public static int dragonMax = 15;
    public static boolean showInfo;

    public static int slashAge = 99;

    public static boolean aiming;
    public static float aimRange = 15f;
    public static int aimStartedTick;

    private ClientState() {}

    public static boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public static boolean glitching() {
        return stage >= 1 && has(SyncPayload.FLAG_GLITCH);
    }

    public static boolean canAct() {
        return tier > 0 && blood > 0f && stage < 1 && !has(SyncPayload.FLAG_GLITCH);
    }

    public static float maxRange() {
        return Math.max(1, dragonMax);
    }

    public static ChronosTier tierEnum() {
        return ChronosTier.byId(tier);
    }

    public static boolean hasChronos() {
        return tier != 0 || dependency > 0 || blood > 0f;
    }

    public static int cooldownTicks() {
        return switch (tierEnum()) {
            case BASE, HEADHUNTER -> bulletCd;
            case ZERO -> holdCd > 0 ? holdCd : bulletCd;
            case DRAGON -> dragonCd;
            default -> 0;
        };
    }

    public static boolean clashGlitch() {
        return has(SyncPayload.FLAG_CLASH_GLITCH);
    }

    public static int percentCost(float base) {
        return Math.round(ChronosLogic.useCost(base, affinity) * 100f);
    }

    public static Component infoText() {
        ChronosTier t = tierEnum();
        return switch (t) {
            case BASE -> Component.translatable("gui.nullmod.info.base", percentCost(ChronosLogic.COST_BULLET));
            case ZERO -> Component.translatable("gui.nullmod.info.zero", percentCost(ChronosLogic.COST_GHOST_START));
            case HEADHUNTER -> Component.translatable("gui.nullmod.info.headhunter", percentCost(ChronosLogic.COST_HUNTER_START));
            case DRAGON -> Component.translatable("gui.nullmod.info.dragon", (int) maxRange(), percentCost(ChronosLogic.COST_DRAGON));
            default -> Component.translatable("gui.nullmod.info.none");
        };
    }

    public static void apply(SyncPayload p) {
        boolean wasImpact = has(SyncPayload.FLAG_IMPACT);
        blood = p.blood();
        tier = p.tier();
        dependency = p.dependency();
        affinity = p.affinity();
        stage = p.stage();
        flags = p.flags();
        cleanseLeft = p.cleanseLeft();
        bulletCd = p.bulletCd();
        holdCd = p.holdCd();
        dragonCd = p.dragonCd();
        dragonMax = p.dragonMax();
        if (!wasImpact && has(SyncPayload.FLAG_IMPACT)) slashAge = 0;
    }

    public static void say(int kind, int arg) {
        speechKind = kind;
        speechArg = arg;
        speechAge = 0;
    }

    public static Component speechText() {
        return switch (speechKind) {
            case SpeechPayload.LOW_1 -> Component.translatable("speech.nullmod.low1");
            case SpeechPayload.LOW_2 -> Component.translatable("speech.nullmod.low2");
            case SpeechPayload.HIGH_1 -> Component.translatable("speech.nullmod.high1");
            case SpeechPayload.HIGH_2 -> Component.translatable("speech.nullmod.high2");
            case SpeechPayload.CLEANSE_BLOCKED -> Component.translatable("speech.nullmod.cleanse_blocked", speechArg);
            case SpeechPayload.NO_WORK -> Component.translatable("speech.nullmod.no_work");
            case SpeechPayload.CAPTAIN -> Component.translatable("speech.nullmod.captain");
            default -> Component.empty();
        };
    }

    public static void tick() {
        if (slashAge < 99) slashAge++;
        if (cleanseLeft > 0) cleanseLeft--;
        if (bulletCd > 0) bulletCd--;
        if (holdCd > 0) holdCd--;
        if (dragonCd > 0) dragonCd--;
        if (speechKind >= 0 && ++speechAge > SPEECH_LIFE) speechKind = -1;
    }

    public static void reset() {
        blood = 0; tier = 0; dependency = 0; affinity = -1; stage = 0; flags = 0;
        cleanseLeft = bulletCd = holdCd = dragonCd = 0;
        speechKind = -1; aiming = false; showInfo = false; slashAge = 99;
    }
}
