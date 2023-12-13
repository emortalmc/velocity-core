package dev.emortal.velocity.player.suggestions;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.grpc.mcplayer.McPlayerProto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public interface UsernameSuggesterProvider {

    @NotNull SuggestionProvider<CommandSource> all();

    @NotNull SuggestionProvider<CommandSource> online();

    @NotNull SuggestionProvider<CommandSource> friends();

    @NotNull SuggestionProvider<CommandSource> custom(@NotNull McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod filterMethod, @Nullable Set<UUID> excludedPlayerIds);
}
