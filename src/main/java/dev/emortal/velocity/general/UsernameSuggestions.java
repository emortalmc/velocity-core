package dev.emortal.velocity.general;

import com.mojang.brigadier.context.CommandContext;
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

public final class UsernameSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsernameSuggestions.class);

    private final McPlayerService playerService;

    public UsernameSuggestions(@Nullable McPlayerService playerService) {
        this.playerService = playerService;
    }

    public @NotNull SuggestionProvider<CommandSource> command(@NotNull FilterMethod filterMethod) {
        return (context, builder) -> this.command(context, builder, filterMethod);
    }

    private @NotNull CompletableFuture<Suggestions> command(@NotNull CommandContext<CommandSource> context, @NotNull SuggestionsBuilder builder,
                                                           @NotNull FilterMethod filterMethod) {
        if (!(context.getSource() instanceof Player player)) return CompletableFuture.completedFuture(builder.build());
        if (this.playerService == null) return CompletableFuture.completedFuture(builder.build());

        String currentInput = builder.getRemainingLowerCase();
        if (currentInput.length() < 3) return CompletableFuture.completedFuture(builder.build());

        return CompletableFuture.supplyAsync(() -> this.suggest(builder, player.getUniqueId(), currentInput, filterMethod));
    }

    private @NotNull Suggestions suggest(@NotNull SuggestionsBuilder builder, @NotNull UUID playerId, @NotNull String currentInput,
                                         @NotNull FilterMethod filterMethod) {
        var pageable = Pageable.newBuilder().setPage(0).setSize(15).build();

        List<McPlayer> players;
        try {
            players = this.playerService.searchPlayersByUsername(playerId, currentInput, pageable, filterMethod);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to retrieve player suggestions", exception);
            return builder.build();
        }

        for (McPlayer mcPlayer : players) {
            builder.suggest(mcPlayer.getCurrentUsername());
        }

        return builder.build();
    }
}
