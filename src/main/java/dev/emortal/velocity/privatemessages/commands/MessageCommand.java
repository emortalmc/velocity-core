package dev.emortal.velocity.privatemessages.commands;


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
import dev.emortal.api.grpc.privatemessage.PrivateMessageGrpc;
import dev.emortal.api.grpc.privatemessage.PrivateMessageProto;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.model.privatemessage.PrivateMessage;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.general.UsernameSuggestions;
import dev.emortal.velocity.privatemessages.LastMessageCache;
import dev.emortal.velocity.utils.CommandUtils;
import io.grpc.Status;
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

    private final LastMessageCache lastMessageCache;
    private final UsernameSuggestions usernameSuggestions;

    private final PrivateMessageGrpc.PrivateMessageFutureStub privateMessageService;
    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;

    public MessageCommand(ProxyServer proxy, UsernameSuggestions usernameSuggestions, LastMessageCache lastMessageCache) {
        this.usernameSuggestions = usernameSuggestions;
        this.lastMessageCache = lastMessageCache;
        this.privateMessageService = GrpcStubCollection.getPrivateMessageService().orElse(null);
        this.mcPlayerService = GrpcStubCollection.getPlayerService().orElse(null);

        proxy.getCommandManager().register("msg", this.createMessageCommand(), "message");
        proxy.getCommandManager().register("r", this.createReplyCommand(), "reply");
    }

    public int sendMessage(Player player, String targetUsername, String message) {
        var playerResponseFuture = this.mcPlayerService.getPlayerByUsername(
                McPlayerProto.PlayerUsernameRequest.newBuilder().setUsername(targetUsername).build()
        );

        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                playerResponse -> {
                    McPlayer mcPlayer = playerResponse.getPlayer();
                    String correctedUsername = mcPlayer.getCurrentUsername();
                    UUID targetId = UUID.fromString(mcPlayer.getId());

                    if (!mcPlayer.getCurrentlyOnline()) {
                        player.sendMessage(Component.text(correctedUsername + " is not currently online.", NamedTextColor.RED)); // todo
                        return;
                    }

                    var messageResponseFuture = this.privateMessageService.sendPrivateMessage(
                            PrivateMessageProto.PrivateMessageRequest.newBuilder()
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
                                player.sendMessage(Component.text("An error occurred while sending your message.", NamedTextColor.RED)); // todo
                                LOGGER.error("An error occurred while sending a private message: ", throwable);
                            }
                    ), ForkJoinPool.commonPool());

                },
                throwable -> {
                    Status status = Status.fromThrowable(throwable);
                    Status.Code code = status.getCode();
                    if (code == Status.Code.NOT_FOUND) {
                        player.sendMessage(Component.text("Could not find player " + targetUsername, NamedTextColor.RED));
                    } else {
                        LOGGER.error("Failed to retrieve player UUID", status.asRuntimeException());
                        player.sendMessage(Component.text("An unknown error occurred", NamedTextColor.RED));
                    }
                }
        ), ForkJoinPool.commonPool());

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

    private BrigadierCommand createMessageCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("message")
                        .requires(CommandUtils.isPlayer())
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
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                                .executes(this::onReplyExecute)
                        )
        );
    }
}
