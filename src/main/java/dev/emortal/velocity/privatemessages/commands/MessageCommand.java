package dev.emortal.velocity.privatemessages.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.grpc.mcplayer.McPlayerProto;
import dev.emortal.api.model.messagehandler.PrivateMessage;
import dev.emortal.api.service.messagehandler.MessageService;
import dev.emortal.api.service.messagehandler.SendPrivateMessageResult;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.general.UsernameSuggestions;
import dev.emortal.velocity.lang.TempLang;
import dev.emortal.velocity.privatemessages.LastMessageCache;
import dev.emortal.velocity.utils.CommandUtils;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class MessageCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageCommand.class);

    private static final String MESSAGE_FORMAT = "<dark_purple>(<light_purple>You -> <username><dark_purple>) <light_purple><message>";

    private static final String YOU_BLOCKED_MESSAGE = "<red>You have blocked <username> so you cannot message them.";
    private static final String THEY_BLOCKED_MESSAGE = "<red><username> has blocked you so you cannot message them.";

    private final LastMessageCache lastMessageCache;
    private final UsernameSuggestions usernameSuggestions;

    private final MessageService messageHandler;

    public MessageCommand(@NotNull ProxyServer proxy, @NotNull MessageService messageHandler, @NotNull UsernameSuggestions usernameSuggestions,
                          @NotNull LastMessageCache lastMessageCache) {
        this.usernameSuggestions = usernameSuggestions;
        this.lastMessageCache = lastMessageCache;
        this.messageHandler = messageHandler;

        proxy.getCommandManager().register("message", this.createMessageCommand(), "msg");
        proxy.getCommandManager().register("reply", this.createReplyCommand(), "r");
    }

    public void sendMessage(@NotNull Player player, @NotNull String targetUsername, @NotNull String message) {
        PlayerResolver.CachedMcPlayer target;
        try {
            target = PlayerResolver.getPlayerData(targetUsername);
        } catch (StatusException exception) {
            Status status = exception.getStatus();
            if (status.getCode() == Status.Code.NOT_FOUND) {
                TempLang.PLAYER_NOT_FOUND.send(player, Placeholder.unparsed("search_username", targetUsername));
                return;
            }

            LOGGER.error("Failed to retrieve player UUID", status.asRuntimeException());
            player.sendMessage(Component.text("An unknown error occurred", NamedTextColor.RED));
            return;
        }

        String correctedUsername = target.username();
        UUID targetId = target.uuid();

        if (!target.online()) {
            player.sendMessage(Component.text(correctedUsername + " is not currently online.", NamedTextColor.RED)); // todo
            return;
        }

        var privateMessage = PrivateMessage.newBuilder()
                .setSenderId(player.getUniqueId().toString())
                .setSenderUsername(player.getUsername())
                .setRecipientId(targetId.toString())
                .setMessage(message)
                .build();

        SendPrivateMessageResult result;
        try {
            result = this.messageHandler.sendPrivateMessage(privateMessage);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while sending a private message: ", exception);
            player.sendMessage(Component.text("An error occurred while sending your message.", NamedTextColor.RED)); // todo
            return;
        }

        var usernamePlaceholder = Placeholder.parsed("username", correctedUsername);
        var responseMessage = switch (result) {
            case SendPrivateMessageResult.Success(PrivateMessage newMessage) ->
                    MINI_MESSAGE.deserialize(MESSAGE_FORMAT, usernamePlaceholder, Placeholder.unparsed("message", message));
            case SendPrivateMessageResult.Error error -> switch (error) {
                case YOU_BLOCKED -> MINI_MESSAGE.deserialize(YOU_BLOCKED_MESSAGE, usernamePlaceholder);
                case PRIVACY_BLOCKED -> MINI_MESSAGE.deserialize(THEY_BLOCKED_MESSAGE, usernamePlaceholder);
                case PLAYER_NOT_ONLINE -> TempLang.PLAYER_NOT_ONLINE.deserialize(usernamePlaceholder);
            };
        };
        player.sendMessage(responseMessage);
    }

    public void onMessageUsage(@NotNull CommandContext<CommandSource> context) {
        context.getSource().sendMessage(Component.text("Usage: /msg <player> <message>", NamedTextColor.RED));
    }

    public void onMessageExecute(@NotNull CommandContext<CommandSource> context) {
        String targetUsername = context.getArgument("receiver", String.class);
        String message = context.getArgument("message", String.class);
        Player player = (Player) context.getSource();

        if (player.getUsername().equalsIgnoreCase(targetUsername)) {
            player.sendMessage(Component.text("You cannot send a message to yourself.", NamedTextColor.RED));
            return;
        }

        this.sendMessage(player, targetUsername, message);
    }

    public void onReplyExecute(@NotNull CommandContext<CommandSource> context) {
        String message = context.getArgument("message", String.class);
        Player player = (Player) context.getSource();

        String targetUsername = this.lastMessageCache.getLastMessageSender(player.getUniqueId());
        if (targetUsername == null) {
            player.sendMessage(Component.text("You have not received any messages yet.", NamedTextColor.RED));
            return;
        }

        this.sendMessage(player, targetUsername, message);
    }

    public void onReplyUsage(@NotNull CommandContext<CommandSource> context) {
        context.getSource().sendMessage(Component.text("Usage: /r <message>", NamedTextColor.RED));
    }

    private @NotNull BrigadierCommand createMessageCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("message")
                        .requires(CommandUtils.isPlayer())
                        .executes(CommandUtils.execute(this::onMessageUsage))
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("receiver", StringArgumentType.word())
                                .suggests((context, builder) -> this.usernameSuggestions.command(context, builder, McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod.ONLINE))
                                .executes(CommandUtils.execute(this::onMessageUsage))
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                                        .executes(CommandUtils.executeAsync(this::onMessageExecute))
                                )
                        )
        );
    }

    public BrigadierCommand createReplyCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("reply")
                        .requires(CommandUtils.isPlayer())
                        .executes(CommandUtils.execute(this::onReplyUsage))
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                                .executes(CommandUtils.executeAsync(this::onReplyExecute))
                        )
        );
    }
}
