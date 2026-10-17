package com.darkgod;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * /darkgod transform [player]  - play the full ascension cinematic
 * /darkgod grant [player]      - instantly become a Dark God
 * /darkgod revoke [player]     - back to mortal
 */
public final class DarkGodCommand {
    private DarkGodCommand() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("darkgod")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("transform")
                                .executes(c -> transform(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(c -> transform(c, EntityArgument.getPlayer(c, "target")))))
                        .then(Commands.literal("grant")
                                .executes(c -> grant(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(c -> grant(c, EntityArgument.getPlayer(c, "target")))))
                        .then(Commands.literal("revoke")
                                .executes(c -> revoke(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(c -> revoke(c, EntityArgument.getPlayer(c, "target"))))));
    }

    private static int transform(CommandContext<CommandSourceStack> c, ServerPlayer p) throws CommandSyntaxException {
        TransformationManager.begin(p);
        c.getSource().sendSuccess(() -> Component.literal("Ascension begun for ").append(p.getDisplayName()), true);
        return 1;
    }

    private static int grant(CommandContext<CommandSourceStack> c, ServerPlayer p) throws CommandSyntaxException {
        DarkPowers.grant(p);
        c.getSource().sendSuccess(() -> Component.literal("Granted Dark God to ").append(p.getDisplayName()), true);
        return 1;
    }

    private static int revoke(CommandContext<CommandSourceStack> c, ServerPlayer p) throws CommandSyntaxException {
        TransformationManager.cancel(p);
        DarkPowers.revoke(p);
        c.getSource().sendSuccess(() -> Component.literal("Revoked Dark God from ").append(p.getDisplayName()), true);
        return 1;
    }
}
