package dev.emortal.velocity.player.suggestions;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.CommandSource;
import org.jetbrains.annotations.NotNull;

public interface UsernameSuggesterProvider {

    @NotNull SuggestionProvider<CommandSource> all();

    @NotNull SuggestionProvider<CommandSource> online();

    @NotNull SuggestionProvider<CommandSource> friends();
}
