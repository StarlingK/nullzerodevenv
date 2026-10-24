package dev.nullmod;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class NullConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue GOV_CHANCE = B
            .comment("% chance a dose of Chronos rolls Tier 2, Government Issue.")
            .translation("nullmod.configuration.governmentIssueChance")
            .defineInRange("governmentIssueChance", 25, 0, 100);

    public static final ModConfigSpec.IntValue DRAGON_CHANCE = B
            .comment("% chance a dose of Chronos rolls Tier 3, The Dragon. Checked before Government Issue.")
            .translation("nullmod.configuration.dragonChance")
            .defineInRange("dragonChance", 8, 0, 100);

    public static final ModConfigSpec.IntValue HUNTER_CHANCE = B
            .comment("% chance a dose of Chronos rolls Headhunter. Checked after Dragon and before Government Issue.")
            .translation("nullmod.configuration.headhunterChance")
            .defineInRange("headhunterChance", 15, 0, 100);

    public static final ModConfigSpec.BooleanValue AFFECT_PLAYERS = B
            .comment("Slow or freeze other players inside the time bubble.")
            .translation("nullmod.configuration.affectOtherPlayers")
            .define("affectOtherPlayers", true);

    public static final ModConfigSpec.IntValue EFFECT_RANGE = B
            .comment("How many blocks around the user time slowdown reaches.")
            .translation("nullmod.configuration.effectRange")
            .defineInRange("effectRange", 12, 0, 30);

    public static final ModConfigSpec.BooleanValue WITHDRAWAL = B
            .comment("Master switch for withdrawal. The nullWithdrawal gamerule must also be true.")
            .translation("nullmod.configuration.withdrawal")
            .define("withdrawal", true);

    public static final ModConfigSpec.BooleanValue DRAGON_PVP = B
            .comment("Dragon blinks can hurt other players.")
            .translation("nullmod.configuration.dragonPvp")
            .define("dragonPvp", true);

    public static final ModConfigSpec.IntValue DRAGON_COOLDOWN = B
            .comment("Seconds between Dragon blinks.")
            .translation("nullmod.configuration.dragonCooldownSeconds")
            .defineInRange("dragonCooldownSeconds", 20, 0, 3600);

    public static final ModConfigSpec.IntValue BULLET_COOLDOWN = B
            .comment("Seconds between Base Bullet Time uses.")
            .translation("nullmod.configuration.bulletTimeCooldownSeconds")
            .defineInRange("bulletTimeCooldownSeconds", 120, 0, 3600);

    public static final ModConfigSpec.IntValue HOLD_COOLDOWN = B
            .comment("Seconds between Zero ghost uses.")
            .translation("nullmod.configuration.ghostCooldownSeconds")
            .defineInRange("ghostCooldownSeconds", 60, 0, 3600);

    public static final ModConfigSpec.IntValue HUNTER_COOLDOWN = B
            .comment("Seconds between Headhunter slowdowns.")
            .translation("nullmod.configuration.headhunterCooldownSeconds")
            .defineInRange("headhunterCooldownSeconds", 60, 0, 3600);

    public static final ModConfigSpec.IntValue DRAGON_MAX_RANGE = B
            .comment("Longest Dragon blink in blocks. The scroll wheel adjusts the blink up to this.")
            .translation("nullmod.configuration.dragonMaxRange")
            .defineInRange("dragonMaxRange", 15, 1, 30);

    public static final ModConfigSpec.DoubleValue CLEANSE_DAYS = B
            .comment("In-game days after a Cleansing potion before another dose is accepted.")
            .translation("nullmod.configuration.cleansingLockoutDays")
            .defineInRange("cleansingLockoutDays", 1.0, 0.0, 30.0);

    public static final ModConfigSpec SPEC = B.build();

    private NullConfig() {}
}
