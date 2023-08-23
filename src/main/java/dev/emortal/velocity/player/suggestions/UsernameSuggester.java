package dev.emortal.velocity.player.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.mcplayer.McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod;
import dev.emortal.api.model.common.Pageable;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class UsernameSuggester implements SuggestionProvider<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsernameSuggester.class);

    // Mojang chose to make Suggestions.empty() return a future and not provide the empty suggestions object
    private static final Suggestions EMPTY = new Suggestions(StringRange.at(0), List.of());

    private final @Nullable McPlayerService playerService;
    private final @NotNull FilterMethod filterMethod;

    UsernameSuggester(@Nullable McPlayerService playerService, @NotNull FilterMethod filterMethod) {
        this.playerService = playerService;
        this.filterMethod = filterMethod;
    }

    @Override
    public @NotNull CompletableFuture<Suggestions> getSuggestions(@NotNull CommandContext<CommandSource> context,
                                                                  @NotNull SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof Player player)) return builder.buildFuture();
        if (this.playerService == null) return builder.buildFuture();

        String input = builder.getRemainingLowerCase();
        if (input.length() < 2) return builder.buildFuture();

        return CompletableFuture.supplyAsync(() -> this.getSuggestions(builder, player.getUniqueId(), input));
    }

    private @NotNull Suggestions getSuggestions(@NotNull SuggestionsBuilder builder, @NotNull UUID playerId, @NotNull String input) {
        Pageable pageable = Pageable.newBuilder().setPage(0).setSize(15).build();

        List<McPlayer> players;
        try {
            players = this.playerService.searchPlayersByUsername(playerId, input, pageable, this.filterMethod);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to retrieve player suggestions", exception);
            return EMPTY;
        }

        for (McPlayer player : players) {
            builder.suggest(player.getCurrentUsername());
        }

        return builder.build();
    }
}
