package dev.emortal.velocity.privatemessages.commands;


import com.google.common.util.concurrent.Futures;
import com.google.protobuf.InvalidProtocolBufferException;
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
import dev.emortal.api.grpc.messagehandler.MessageHandlerGrpc;
import dev.emortal.api.grpc.messagehandler.MessageHandlerProto;
import dev.emortal.api.model.messagehandler.PrivateMessage;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.general.UsernameSuggestions;
import dev.emortal.velocity.lang.TempLang;
import dev.emortal.velocity.privatemessages.LastMessageCache;
import dev.emortal.velocity.utils.CommandUtils;
import io.grpc.Status;
import io.grpc.protobuf.StatusProto;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

public class MessageCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageCommand.class);

    private static final String MESSAGE_FORMAT = "<dark_purple>(<light_purple>You -> <username><dark_purple>) <light_purple><message>";

    private static final String YOU_BLOCKED_MESSAGE = "<red>You have blocked <username> so you cannot message them.";
    private static final String THEY_BLOCKED_MESSAGE = "<red><username> has blocked you so you cannot message them.";

    private final LastMessageCache lastMessageCache;
    private final UsernameSuggestions usernameSuggestions;

    private final MessageHandlerGrpc.MessageHandlerFutureStub messageHandler;
    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;

    public MessageCommand(ProxyServer proxy, UsernameSuggestions usernameSuggestions, LastMessageCache lastMessageCache) {
        this.usernameSuggestions = usernameSuggestions;
        this.lastMessageCache = lastMessageCache;
        this.messageHandler = GrpcStubCollection.getMessageHandlerService().orElse(null);
        this.mcPlayerService = GrpcStubCollection.getPlayerService().orElse(null);

        proxy.getCommandManager().register("message", this.createMessageCommand(), "msg");
        proxy.getCommandManager().register("reply", this.createReplyCommand(), "r");
    }

    public int sendMessage(Player player, String targetUsername, String message) {
        PlayerResolver.retrievePlayerData(targetUsername, playerResponse -> {
            String correctedUsername = playerResponse.username();
            UUID targetId = playerResponse.uuid();

            if (!playerResponse.online()) {
                player.sendMessage(Component.text(correctedUsername + " is not currently online.", NamedTextColor.RED)); // todo
                return;
            }

            var messageResponseFuture = this.messageHandler.sendPrivateMessage(
                    MessageHandlerProto.PrivateMessageRequest.newBuilder()
                            .setMessage(
                                    PrivateMessage.newBuilder()
                                            .setSenderId(player.getUniqueId().toString())
                                            .setSenderUsername(player.getUsername())
                                            .setRecipientId(targetId.toString())
                                            .setMessage(message)
                            ).build()
            );

            Futures.addCallback(messageResponseFuture, FunctionalFutureCallback.create(
                    messageResponse -> {
                        player.sendMessage(MINI_MESSAGE.deserialize(MESSAGE_FORMAT,
                                Placeholder.parsed("username", correctedUsername),
                                Placeholder.unparsed("message", message)
                        ));
                    },
                    throwable -> {
                        com.google.rpc.Status status = StatusProto.fromThrowable(throwable);
                        if (status == null || status.getDetailsCount() == 0) {
                            player.sendMessage(Component.text("An error occurred while sending your message.", NamedTextColor.RED)); // todo
                            LOGGER.error("An error occurred while sending a private message: ", throwable);
                            return;
                        }

                        try {
                            MessageHandlerProto.PrivateMessageErrorResponse errorResponse = status.getDetails(0)
                                    .unpack(MessageHandlerProto.PrivateMessageErrorResponse.class);

                            player.sendMessage(switch (errorResponse.getReason()) {
                                case YOU_BLOCKED -> MINI_MESSAGE.deserialize(YOU_BLOCKED_MESSAGE,
                                        Placeholder.parsed("username", correctedUsername)
                                );
                                case PRIVACY_BLOCKED -> MINI_MESSAGE.deserialize(THEY_BLOCKED_MESSAGE,
                                        Placeholder.parsed("username", correctedUsername)
                                );
                                case PLAYER_NOT_ONLINE ->
                                        TempLang.PLAYER_NOT_ONLINE.deserialize(Placeholder.unparsed("username", correctedUsername));
                                default -> {
                                    LOGGER.error("An error occurred while sending a private message: ", throwable);
                                    yield Component.text("An error occurred while sending your message.", NamedTextColor.RED);
                                }
                            });
                        } catch (InvalidProtocolBufferException e) {
                            player.sendMessage(Component.text("An error occurred while sending your message.", NamedTextColor.RED)); // todo
                            LOGGER.error("An error occurred while sending a private message: ", throwable);
                        }
                    }
            ), ForkJoinPool.commonPool());
        }, status -> {
            Status.Code code = status.getCode();
            if (code == Status.Code.NOT_FOUND) {
                TempLang.PLAYER_NOT_FOUND.send(player, Placeholder.unparsed("search_username", targetUsername));
                return;
            }

            LOGGER.error("Failed to retrieve player UUID", status.asRuntimeException());
            player.sendMessage(Component.text("An unknown error occurred", NamedTextColor.RED));
        });
        return 1;
    }

    public int onMessageUsage(CommandContext<CommandSource> context) {
        context.getSource().sendMessage(Component.text("Usage: /msg <player> <message>", NamedTextColor.RED));
        return 1;
    }

    public int onMessageExecute(CommandContext<CommandSource> context) {
        String targetUsername = context.getArgument("receiver", String.class);
        String message = context.getArgument("message", String.class);
        Player player = (Player) context.getSource();

        if (player.getUsername().equalsIgnoreCase(targetUsername)) {
            player.sendMessage(Component.text("You cannot send a message to yourself.", NamedTextColor.RED));
            return 1;
        }

        return this.sendMessage(player, targetUsername, message);
    }

    public int onReplyExecute(CommandContext<CommandSource> context) {
        String message = context.getArgument("message", String.class);
        Player player = (Player) context.getSource();

        String targetUsername = this.lastMessageCache.getLastMessageSender(player.getUniqueId());
        if (targetUsername == null) {
            player.sendMessage(Component.text("You have not received any messages yet.", NamedTextColor.RED));
            return 1;
        }
        return this.sendMessage(player, targetUsername, message);
    }

    public int onReplyUsage(CommandContext<CommandSource> context) {
        context.getSource().sendMessage(Component.text("Usage: /r <message>", NamedTextColor.RED));
        return 1;
    }

    private BrigadierCommand createMessageCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("message")
                        .requires(CommandUtils.isPlayer())
                        .executes(this::onMessageUsage)
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("receiver", StringArgumentType.word())
                                .suggests((context, builder) -> this.usernameSuggestions.command(context, builder, McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod.ONLINE))
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                                        .executes(this::onMessageExecute)
                                )
                        )
        );
    }

    public BrigadierCommand createReplyCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("reply")
                        .requires(CommandUtils.isPlayer())
                        .executes(this::onReplyUsage)
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                                .executes(this::onReplyExecute)
                        )
        );
    }
}
