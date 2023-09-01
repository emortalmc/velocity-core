package dev.emortal.velocity.privatemessages.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.suggestions.UsernameSuggesterProvider;
import org.jetbrains.annotations.NotNull;

public final class MessageCommand extends EmortalCommand implements EmortalCommandExecutor {

    private final MessageSender messageSender;

    public MessageCommand(@NotNull MessageSender messageSender, @NotNull UsernameSuggesterProvider usernameSuggesters) {
        super("message", "msg");
        this.messageSender = messageSender;

        super.setPlayerOnly();
        super.setDefaultExecutor(this::sendUsage);

        var receiverArgument = argument("receiver", StringArgumentType.word(), usernameSuggesters.online());
        super.addSyntax(this::sendUsage, receiverArgument);

        var messageArgument = argument("message", StringArgumentType.greedyString(), null);
        super.addSyntax(this, receiverArgument, messageArgument);
    }

    private void sendUsage(@NotNull CommandContext<CommandSource> context) {
        ChatMessages.MESSAGE_USAGE.send(context.getSource());
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player player = (Player) source;
        String targetUsername = arguments.getArgument("receiver", String.class);
        String message = arguments.getArgument("message", String.class);

        if (player.getUsername().equalsIgnoreCase(targetUsername)) {
            ChatMessages.ERROR_CANNOT_MESSAGE_SELF.send(player);
            return;
        }

        this.messageSender.sendMessage(player, targetUsername, message);
    }
}
