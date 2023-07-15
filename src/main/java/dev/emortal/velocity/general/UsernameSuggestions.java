package dev.emortal.velocity.general;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.mcplayer.McPlayerProto;
import dev.emortal.api.model.common.Pageable;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.utils.GrpcStubCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UsernameSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsernameSuggestions.class);

    private final McPlayerService playerService;

    public UsernameSuggestions() {
        this.playerService = GrpcStubCollection.getPlayerService().orElse(null);
    }

    public SuggestionProvider<CommandSource> command(McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod filterMethod) {
        return (context, builder) -> this.command(context, builder, filterMethod);
    }

    public CompletableFuture<Suggestions> command(
            CommandContext<CommandSource> context, SuggestionsBuilder builder,
            McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod filterMethod) {

        if (!(context.getSource() instanceof Player player)) return CompletableFuture.completedFuture(builder.build());

        String currentInput = builder.getRemainingLowerCase();
        if (currentInput.length() < 3) return CompletableFuture.completedFuture(builder.build());

        return CompletableFuture.supplyAsync(() -> {
            var pageable = Pageable.newBuilder().setPage(0).setSize(15).build();
            List<McPlayer> players = this.playerService.searchPlayersByUsername(player.getUniqueId(), currentInput, pageable, filterMethod);

            for (McPlayer mcPlayer : players) {
                builder.suggest(mcPlayer.getCurrentUsername());
            }

            return builder.build();
        }).exceptionally(throwable -> {
            LOGGER.error("Failed to retrieve player suggestions", throwable);
            return builder.build();
        });
    }
}
