package dev.emortal.velocity.party.commands.subs;

import com.velocitypowered.api.command.CommandSource;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import org.jetbrains.annotations.NotNull;

/*
 * This is currently a W.I.P, and is not yet implemented.
 *
 * It exists because we need it to send the help message, and because it will make adding sub commands easier in the future.
 */
public final class PartySettingsSub implements EmortalCommandExecutor {

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        ChatMessages.PARTY_SETTINGS_HELP.send(source);
    }
}
