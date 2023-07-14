package dev.emortal.velocity.relationships.commands.block;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.grpc.mcplayer.McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod;
import dev.emortal.api.grpc.relationship.RelationshipProto;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.DeleteBlockResult;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.general.UsernameSuggestions;
import dev.emortal.velocity.utils.CommandUtils;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public final class BlockCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockCommand.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component BLOCK_USAGE_MESSAGE = MINI_MESSAGE.deserialize("<red>Usage: /block <username>");
    private static final Component UNBLOCK_USAGE_MESSAGE = MINI_MESSAGE.deserialize("<red>Usage: /unblock <username>");

    private static final String BLOCKED_PLAYERS_MESSAGE = "<red>Blocked Players (<count>): <blocked_players></red>";

    private final RelationshipService relationshipService;
    private final McPlayerService mcPlayerService;

    private final UsernameSuggestions usernameSuggestions;

    public BlockCommand(@NotNull ProxyServer proxy, @NotNull McPlayerService mcPlayerService, @NotNull RelationshipService relationshipService,
                        @NotNull UsernameSuggestions usernameSuggestions) {
        this.relationshipService = relationshipService;
        this.mcPlayerService = mcPlayerService;
        this.usernameSuggestions = usernameSuggestions;

        proxy.getCommandManager().register(this.createBlockCommand());
        proxy.getCommandManager().register(this.createUnblockCommand());
        proxy.getCommandManager().register(this.createListBlocksCommand());
    }

    private @NotNull BrigadierCommand createBlockCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("block")
                        .requires(CommandUtils.isPlayer())
                        .executes(context -> {
                            context.getSource().sendMessage(BLOCK_USAGE_MESSAGE);
                            return 1;
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                                .suggests((context, builder) -> this.usernameSuggestions.command(context, builder, FilterMethod.NONE))
                                .executes(CommandUtils.executeAsync(this::block)))
        );
    }

    private @NotNull BrigadierCommand createUnblockCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("unblock")
                        .requires(CommandUtils.isPlayer())
                        .executes(context -> {
                            context.getSource().sendMessage(UNBLOCK_USAGE_MESSAGE);
                            return 1;
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                                .suggests((context, builder) -> this.usernameSuggestions.command(context, builder, FilterMethod.NONE))
                                .executes(CommandUtils.executeAsync(this::unblock)))
        );
    }

    private @NotNull BrigadierCommand createListBlocksCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("listblocks")
                        .requires(CommandUtils.isPlayer())
                        .executes(CommandUtils.executeAsync(this::listBlocks))
        );
    }

    private void block(@NotNull CommandContext<CommandSource> context) {
        Player sender = (Player) context.getSource();
        String targetUsername = StringArgumentType.getString(context, "username");

        McPlayer target;
        try {
            target = this.mcPlayerService.getPlayerByUsername(targetUsername);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while trying to block the player", exception);
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to block the player"));
            return;
        }

        if (target == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Player not found"));
            return;
        }

        UUID targetId = UUID.fromString(target.getId());
        if (targetId.equals(sender.getUniqueId())) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>You can't block yourself"));
            return;
        }

        RelationshipProto.CreateBlockResponse.CreateBlockResult result;
        try {
            result = this.relationshipService.block(sender.getUniqueId(), targetId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while trying to block the player", exception);
            sender.sendMessage(Component.text("An error occurred while trying to block " + targetUsername));
            return;
        }

        var message = switch (result) {
            case SUCCESS -> Component.text("You have blocked " + targetUsername, NamedTextColor.GREEN);
            case ALREADY_BLOCKED -> Component.text("You have already blocked " + targetUsername, NamedTextColor.RED);
            case FAILED_FRIENDS -> Component.text("You must unfriend " + targetUsername + " before blocking them", NamedTextColor.RED);
            case UNRECOGNIZED -> Component.text("An error occurred while trying to block " + targetUsername);
        };
        sender.sendMessage(message);
    }

    private void unblock(@NotNull CommandContext<CommandSource> context) {
        Player sender = (Player) context.getSource();
        String targetUsername = StringArgumentType.getString(context, "username");

        McPlayer target;
        try {
            target = this.mcPlayerService.getPlayerByUsername(targetUsername);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while trying to unblock the player", exception);
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to unblock the player"));
            return;
        }

        if (target == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Player not found"));
            return;
        }

        UUID targetId = UUID.fromString(target.getId());
        if (targetId.equals(sender.getUniqueId())) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>You can't unblock yourself"));
            return;
        }

        DeleteBlockResult result;
        try {
            result = this.relationshipService.unblock(sender.getUniqueId(), targetId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while trying to unblock the player", exception);
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to unblock the player"));
            return;
        }

        var message = switch (result) {
            case SUCCESS -> Component.text(target.getCurrentUsername() + " has been unblocked");
            case NOT_BLOCKED -> MINI_MESSAGE.deserialize("<red>You have not blocked the player");
        };
        sender.sendMessage(message);
    }

    private void listBlocks(CommandContext<CommandSource> context) {
        Player sender = (Player) context.getSource();

        List<UUID> blockedIds;
        try {
            blockedIds = this.relationshipService.getBlockedList(sender.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while trying to list your blocked players", exception);
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to list your blocked players"));
            return;
        }

        if (blockedIds.isEmpty()) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>You have not blocked any players"));
            return;
        }

        List<McPlayer> blockedPlayers;
        try {
            blockedPlayers = this.mcPlayerService.getPlayersById(blockedIds);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while trying to list your blocked players", exception);
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to list your blocked players"));
            return;
        }

        String blockedNames = String.join(", ", blockedPlayers.stream()
                .map(McPlayer::getCurrentUsername)
                .toList());

        Component message = MINI_MESSAGE.deserialize(BLOCKED_PLAYERS_MESSAGE,
                Placeholder.unparsed("count", String.valueOf(blockedPlayers.size())),
                Placeholder.unparsed("blocked_players", blockedNames));

        sender.sendMessage(message);
    }
}
