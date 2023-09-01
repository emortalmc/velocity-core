package dev.emortal.velocity.player.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.player.SessionCache;
import dev.emortal.velocity.player.suggestions.UsernameSuggesterProvider;
import org.jetbrains.annotations.NotNull;

public final class PlaytimeCommand extends EmortalCommand {

    public PlaytimeCommand(@NotNull McPlayerService playerService, @NotNull SessionCache sessionCache,
                           @NotNull UsernameSuggesterProvider usernameSuggesters) {
        super("playtime");

        super.setPlayerOnly();
        super.setDefaultExecutor(new SelfPlaytimeCommand(playerService, sessionCache));

        var usernameArgument = argument("username", StringArgumentType.word(), usernameSuggesters.all());
        super.addSyntax(new OtherPlaytimeCommand(playerService), usernameArgument);
    }
}
