package dev.emortal.velocity.privatemessages.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.command.CommandConditions;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.privatemessages.LastMessageCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public final class ReplyCommand extends EmortalCommand {

    private final MessageSender messageSender;
    private final LastMessageCache lastMessageCache;

    public ReplyCommand(@NotNull MessageSender messageSender, @NotNull LastMessageCache lastMessageCache) {
        super("reply", "r");
        this.messageSender = messageSender;
        this.lastMessageCache = lastMessageCache;

        super.setCondition(CommandConditions.playerOnly());
        super.setDefaultExecutor(this::sendUsage);

        var messageArgument = argument("message", StringArgumentType.greedyString(), null);
        super.addSyntax(this::execute, messageArgument);
    }

    private void sendUsage(@NotNull CommandContext<CommandSource> context) {
        context.getSource().sendMessage(Component.text("Usage: /r <message>", NamedTextColor.RED));
    }

    private void execute(@NotNull CommandContext<CommandSource> context) {
        String message = context.getArgument("message", String.class);
        Player player = (Player) context.getSource();

        String targetUsername = this.lastMessageCache.getLastMessageSender(player.getUniqueId());
        if (targetUsername == null) {
            player.sendMessage(Component.text("You have not received any messages yet.", NamedTextColor.RED));
            return;
        }

        this.messageSender.sendMessage(player, targetUsername, message);
    }
}
