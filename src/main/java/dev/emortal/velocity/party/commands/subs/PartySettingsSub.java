package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.command.CommandExecutor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

/*
 * This is currently a W.I.P, and is not yet implemented.
 *
 * It exists because we need it to send the help message, and because it will make adding sub commands easier in the future.
 */
public final class PartySettingsSub implements CommandExecutor<CommandSource> {

    private static final Component HELP_MESSAGE = MiniMessage.miniMessage().deserialize("""
            <light_purple>----- Party Settings Help -----
            /party settings
            /party settings <setting> <value>
            ---------------------------</light_purple>""");

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        context.getSource().sendMessage(HELP_MESSAGE);
    }
}
