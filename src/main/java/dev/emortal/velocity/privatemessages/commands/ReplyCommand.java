package dev.emortal.velocity.privatemessages.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.CommandConditions;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.privatemessages.LastMessageCache;
import org.jetbrains.annotations.NotNull;

public final class ReplyCommand extends EmortalCommand implements EmortalCommandExecutor {

    private final MessageSender messageSender;
    private final LastMessageCache lastMessageCache;

    public ReplyCommand(@NotNull MessageSender messageSender, @NotNull LastMessageCache lastMessageCache) {
        super("reply", "r");
        this.messageSender = messageSender;
        this.lastMessageCache = lastMessageCache;

        super.setCondition(CommandConditions.playerOnly());
        super.setDefaultExecutor(this::sendUsage);

        var messageArgument = argument("message", StringArgumentType.greedyString(), null);
        super.addSyntax(this, messageArgument);
    }

    private void sendUsage(@NotNull CommandContext<CommandSource> context) {
        ChatMessages.REPLY_USAGE.send(context.getSource());
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player player = (Player) source;
        String message = arguments.getArgument("message", String.class);

        String targetUsername = this.lastMessageCache.getLastMessageSender(player.getUniqueId());
        if (targetUsername == null) {
            ChatMessages.ERROR_NO_ONE_TO_REPLY_TO.send(player);
            return;
        }

        this.messageSender.sendMessage(player, targetUsername, message);
    }
}
