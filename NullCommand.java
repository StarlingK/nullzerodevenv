package dev.nullmod;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import dev.nullmod.core.AbilityManager;
import dev.nullmod.core.ChronosData;
import dev.nullmod.core.ChronosLogic;
import dev.nullmod.core.ChronosTier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = NullMod.MODID)
public final class NullCommand {
    private static final List<String> TIER_NAMES = List.of("random", "base", "zero", "headhunter", "dragon");
    private static final SimpleCommandExceptionType BAD_TIER =
            new SimpleCommandExceptionType(Component.translatable("commands.nullmod.bad_tier"));

    private NullCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("null")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("withdrawal")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100))
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            setBar(target, amount);
                                            context.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.nullmod.withdrawal", target.getDisplayName(), amount), true);
                                            return amount;
                                        }))))
                .then(Commands.literal("chronos")
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> give(context, "random"))
                                        .then(Commands.argument("tier", StringArgumentType.word())
                                                .suggests((context, builder) ->
                                                        SharedSuggestionProvider.suggest(TIER_NAMES, builder))
                                                .executes(context -> give(context,
                                                        StringArgumentType.getString(context, "tier"))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(NullCommand::remove))))
                .then(Commands.literal("cutscene")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(NullCommand::cutscene))));
    }

    private static int give(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int tier;
        switch (name.toLowerCase()) {
            case "random" -> tier = ChronosLogic.rollTier(target.getRandom());
            case "base" -> tier = ChronosTier.BASE.id;
            case "zero" -> tier = ChronosTier.ZERO.id;
            case "headhunter" -> tier = ChronosTier.HEADHUNTER.id;
            case "dragon" -> tier = ChronosTier.DRAGON.id;
            default -> throw BAD_TIER.create();
        }
        ChronosLogic.applyDose(target, tier);
        ChronosTier given = ChronosTier.byId(tier);
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands.nullmod.give", target.getDisplayName(), given.displayName()), true);
        return tier;
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ChronosLogic.removeChronos(target);
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands.nullmod.remove", target.getDisplayName()), true);
        return 1;
    }

    private static int cutscene(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ChronosLogic.startCutscene(target, ChronosLogic.data(target), target.serverLevel().getGameTime());
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands.nullmod.cutscene", target.getDisplayName()), true);
        return 1;
    }

    private static void setBar(ServerPlayer player, int percent) {
        ChronosData d = ChronosLogic.data(player);
        AbilityManager.endAll(player, d, false);
        d.blood = percent / 100.0f;
        d.withdrawalTicks = 0;
        d.glitchEnd = 0;
        d.cueDone = false;
        if (d.tier == 0) {
            d.tier = ChronosTier.BASE.id;
        }

        d.dependency = Math.max(Math.max(1, ChronosTier.rankOf(d.tier)), d.dependency);
        if (d.affinity < 0) {
            d.affinity = 1 + player.getRandom().nextInt(100);
        }
        ChronosLogic.syncNow(player);
    }
}
