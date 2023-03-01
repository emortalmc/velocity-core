package dev.emortal.velocity.relationships.commands.block;

import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.grpc.mcplayer.McPlayerGrpc;
import dev.emortal.api.grpc.mcplayer.McPlayerProto;
import dev.emortal.api.grpc.relationship.RelationshipGrpc;
import dev.emortal.api.grpc.relationship.RelationshipProto;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.model.relationship.PlayerBlock;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.general.UsernameSuggestions;
import dev.emortal.velocity.utils.CommandUtils;
import io.grpc.Status;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

public class BlockCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockCommand.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component BLOCK_USAGE_MESSAGE = MINI_MESSAGE.deserialize("<red>Usage: /block <username>");
    private static final Component UNBLOCK_USAGE_MESSAGE = MINI_MESSAGE.deserialize("<red>Usage: /unblock <username>");

    private static final String BLOCKED_PLAYERS_MESSAGE = "<red>Blocked Players (<count>): <blocked_players></red>";

    private final RelationshipGrpc.RelationshipFutureStub relationshipService = GrpcStubCollection.getRelationshipService().orElse(null);
    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService = GrpcStubCollection.getPlayerService().orElse(null);

    private final UsernameSuggestions usernameSuggestions;

    public BlockCommand(@NotNull ProxyServer proxy, UsernameSuggestions usernameSuggestions) {
        this.usernameSuggestions = usernameSuggestions;

        proxy.getCommandManager().register(this.createBlockCommand());
        proxy.getCommandManager().register(this.createUnblockCommand());
        proxy.getCommandManager().register(this.createListBlocksCommand());
    }

    private BrigadierCommand createBlockCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("block")
                        .requires(CommandUtils.isPlayer())
                        .executes(context -> {
                            context.getSource().sendMessage(BLOCK_USAGE_MESSAGE);
                            return 1;
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                                .suggests((context, builder) -> this.usernameSuggestions.command(context, builder, McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod.NONE))
                                .executes(this::block))
        );
    }

    private BrigadierCommand createUnblockCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("unblock")
                        .requires(CommandUtils.isPlayer())
                        .executes(context -> {
                            context.getSource().sendMessage(UNBLOCK_USAGE_MESSAGE);
                            return 1;
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                                .suggests((context, builder) -> this.usernameSuggestions.command(context, builder, McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod.NONE))
                                .executes(this::unblock))
        );
    }

    private BrigadierCommand createListBlocksCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("listblocks")
                        .requires(CommandUtils.isPlayer())
                        .executes(this::listBlocks)
        );
    }

    private int block(CommandContext<CommandSource> context) {
        if (this.relationshipService == null) {
            context.getSource().sendMessage(MINI_MESSAGE.deserialize("<red>Friend service is not available"));
            return 1;
        }

        Player sender = (Player) context.getSource();
        String targetUsername = StringArgumentType.getString(context, "username");

        var playerResponseFuture = this.mcPlayerService.getPlayerByUsername(
                McPlayerProto.PlayerUsernameRequest.newBuilder().setUsername(targetUsername).build());

        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                playerResponse -> {
                    UUID targetId = UUID.fromString(playerResponse.getPlayer().getId());
                    if (targetId.equals(sender.getUniqueId())) {
                        sender.sendMessage(MINI_MESSAGE.deserialize("<red>You can't block yourself"));
                        return;
                    }

                    var blockResponseFuture = this.relationshipService.createBlock(
                            RelationshipProto.CreateBlockRequest.newBuilder()
                                    .setBlock(
                                            PlayerBlock.newBuilder()
                                                    .setBlockedId(targetId.toString())
                                                    .setBlockerId(sender.getUniqueId().toString())
                                    )
                                    .build());

                    Futures.addCallback(blockResponseFuture, FunctionalFutureCallback.create(
                                    blockResponse -> {
                                        RelationshipProto.CreateBlockResponse.CreateBlockResult result = blockResponse.getResult();

                                        sender.sendMessage(switch (result) {
                                            case SUCCESS ->
                                                    Component.text("You have blocked " + targetUsername, NamedTextColor.GREEN);
                                            case ALREADY_BLOCKED ->
                                                    Component.text("You have already blocked " + targetUsername, NamedTextColor.RED);
                                            case FAILED_FRIENDS ->
                                                    Component.text("You must unfriend " + targetUsername + " before blocking them", NamedTextColor.RED);
                                            default ->
                                                    Component.text("An error occurred while trying to block " + targetUsername, NamedTextColor.RED);
                                        });
                                    },
                                    throwable -> {
                                        sender.sendMessage(Component.text("An error occurred while trying to block " + targetUsername));
                                        LOGGER.error("An error occurred while trying to block the player", throwable);
                                    }),
                            ForkJoinPool.commonPool());
                },
                throwable -> {
                    Status status = Status.fromThrowable(throwable);
                    if (status.getCode() == Status.Code.NOT_FOUND) {
                        sender.sendMessage(MINI_MESSAGE.deserialize("<red>Player not found"));
                    } else {
                        sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to block the player"));
                        LOGGER.error("An error occurred while trying to block the player", throwable);
                    }
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }

    private int unblock(CommandContext<CommandSource> context) {
        if (this.relationshipService == null) {
            context.getSource().sendMessage(MINI_MESSAGE.deserialize("<red>Friend service is not available"));
            return 1;
        }

        Player sender = (Player) context.getSource();
        String targetUsername = StringArgumentType.getString(context, "username");

        var playerResponseFuture = this.mcPlayerService.getPlayerByUsername(
                McPlayerProto.PlayerUsernameRequest.newBuilder().setUsername(targetUsername).build());

        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                playerResponse -> {
                    McPlayer targetPlayer = playerResponse.getPlayer();

                    UUID targetId = UUID.fromString(targetPlayer.getId());
                    if (targetId.equals(sender.getUniqueId())) {
                        sender.sendMessage(MINI_MESSAGE.deserialize("<red>You can't unblock yourself"));
                        return;
                    }

                    var unblockResponseFuture = this.relationshipService.deleteBlock(
                            RelationshipProto.DeleteBlockRequest.newBuilder()
                                    .setIssuerId(sender.getUniqueId().toString())
                                    .setTargetId(targetId.toString())
                                    .build());

                    Futures.addCallback(unblockResponseFuture, FunctionalFutureCallback.create(
                                    unblockResponse -> {
                                        sender.sendMessage(Component.text(targetPlayer.getCurrentUsername() + " has been unblocked"));
                                    },
                                    throwable -> {
                                        Status status = Status.fromThrowable(throwable);
                                        if (status.getCode() == Status.Code.NOT_FOUND) {
                                            sender.sendMessage(MINI_MESSAGE.deserialize("<red>You have not blocked the player"));
                                            return;
                                        }
                                        sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to unblock the player"));
                                        LOGGER.error("An error occurred while trying to unblock the player", throwable);
                                    }),
                            ForkJoinPool.commonPool());
                },
                throwable -> {
                    Status status = Status.fromThrowable(throwable);
                    if (status.getCode() == Status.Code.NOT_FOUND) {
                        sender.sendMessage(MINI_MESSAGE.deserialize("<red>Player not found"));
                    } else {
                        sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to unblock the player"));
                        LOGGER.error("An error occurred while trying to unblock the player", throwable);
                    }
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }

    private int listBlocks(CommandContext<CommandSource> context) {
        if (this.relationshipService == null) {
            context.getSource().sendMessage(MINI_MESSAGE.deserialize("<red>Friend service is not available"));
            return 1;
        }

        Player sender = (Player) context.getSource();

        var listBlocksResponseFuture = this.relationshipService.getBlockedList(
                RelationshipProto.GetBlockedListRequest.newBuilder()
                        .setPlayerId(sender.getUniqueId().toString())
                        .build());

        Futures.addCallback(listBlocksResponseFuture, FunctionalFutureCallback.create(
                        response -> {
                            List<UUID> blockedIds = response.getBlockedPlayerIdsList()
                                    .stream()
                                    .map(UUID::fromString)
                                    .toList();

                            if (blockedIds.isEmpty()) {
                                sender.sendMessage(MINI_MESSAGE.deserialize("<red>You have not blocked any players"));
                                return;
                            }

                            var playerReqFuture = this.mcPlayerService.getPlayers(McPlayerProto.GetPlayersRequest.newBuilder()
                                    .addAllPlayerIds(response.getBlockedPlayerIdsList())
                                    .build());

                            Futures.addCallback(playerReqFuture, FunctionalFutureCallback.create(
                                            playersResponse -> {
                                                List<McPlayer> blockedPlayers = playersResponse.getPlayersList();

                                                String blockedNames = String.join(", ", blockedPlayers.stream()
                                                        .map(McPlayer::getCurrentUsername)
                                                        .toList());

                                                Component message = MINI_MESSAGE.deserialize(BLOCKED_PLAYERS_MESSAGE,
                                                        Placeholder.unparsed("count", String.valueOf(blockedPlayers.size())),
                                                        Placeholder.unparsed("blocked_players", blockedNames));

                                                sender.sendMessage(message);
                                            },
                                            throwable -> {
                                                sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to list your blocked players"));
                                                LOGGER.error("An error occurred while trying to list your blocked players", throwable);
                                            }),
                                    ForkJoinPool.commonPool());

                        },
                        throwable -> {
                            sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to list your blocked players"));
                            LOGGER.error("An error occurred while trying to list your blocked players", throwable);
                        }),
                ForkJoinPool.commonPool());

        return 1;
    }
}
