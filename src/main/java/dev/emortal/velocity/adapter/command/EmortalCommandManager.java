package dev.emortal.velocity.adapter.command;

import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.player.suggestions.UsernameSuggesterProvider;
import org.jetbrains.annotations.NotNull;

public interface EmortalCommandManager {

    @NotNull UsernameSuggesterProvider usernameSuggesters();

    void registerCommand(@NotNull EmortalCommand command);
}
