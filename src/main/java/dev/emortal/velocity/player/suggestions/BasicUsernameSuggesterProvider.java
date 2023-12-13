package dev.emortal.velocity.player.suggestions;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.grpc.mcplayer.McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod;
import dev.emortal.api.service.mcplayer.McPlayerService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public final class BasicUsernameSuggesterProvider implements UsernameSuggesterProvider {

    private final @Nullable McPlayerService playerService;

    private final @NotNull SuggestionProvider<CommandSource> all;
    private final @NotNull SuggestionProvider<CommandSource> online;
    private final @NotNull SuggestionProvider<CommandSource> friends;

    public BasicUsernameSuggesterProvider(@Nullable McPlayerService playerService) {
        this.playerService = playerService;

        this.all = new UsernameSuggester(playerService, FilterMethod.NONE);
        this.online = new UsernameSuggester(playerService, FilterMethod.ONLINE);
        this.friends = new UsernameSuggester(playerService, FilterMethod.FRIENDS);
    }

    @Override
    public @NotNull SuggestionProvider<CommandSource> all() {
        return this.all;
    }

    @Override
    public @NotNull SuggestionProvider<CommandSource> online() {
        return this.online;
    }

    @Override
    public @NotNull SuggestionProvider<CommandSource> friends() {
        return this.friends;
    }

    public @NotNull SuggestionProvider<CommandSource> custom(@NotNull FilterMethod filterMethod, @Nullable Set<UUID> excludedPlayerIds) {
        return new UsernameSuggester(this.playerService, filterMethod, excludedPlayerIds);
    }
}
