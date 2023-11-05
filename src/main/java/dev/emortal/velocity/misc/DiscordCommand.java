package dev.emortal.velocity.misc;

import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.lang.ChatMessages;

public class DiscordCommand extends EmortalCommand {
    public DiscordCommand() {
        super("discord");

        super.setDefaultExecutor(context -> context.getSource().sendMessage(ChatMessages.DISCORD_COMMAND.get()));
    }
}
