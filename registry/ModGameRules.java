package dev.nullmod.registry;

import net.minecraft.world.level.GameRules;

public final class ModGameRules {

    public static final GameRules.Key<GameRules.BooleanValue> DEPLETION =
            GameRules.register("nullDepletion", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

    public static final GameRules.Key<GameRules.BooleanValue> WITHDRAWAL =
            GameRules.register("nullWithdrawal", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

    public static final GameRules.Key<GameRules.BooleanValue> NO_COOLDOWNS =
            GameRules.register("nullNoCooldowns", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

    public static void init() {}

    private ModGameRules() {}
}
