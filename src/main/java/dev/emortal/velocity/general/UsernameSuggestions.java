package dev.emortal.velocity.general;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.mcplayer.McPlayerGrpc;
import dev.emortal.api.grpc.mcplayer.McPlayerProto;
import dev.emortal.api.model.common.Pageable;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.utils.GrpcStubCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class UsernameSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsernameSuggestions.class);

    private final McPlayerGrpc.McPlayerFutureStub playerService;

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
            try {
                var response = this.playerService.searchPlayersByUsername(
                        McPlayerProto.SearchPlayersByUsernameRequest.newBuilder()
                                .setIssuerId(player.getUniqueId().toString())
                                .setSearchUsername(currentInput)
                                .setFilterMethod(filterMethod)
                                .setPageable(
                                        Pageable.newBuilder()
                                                .setPage(0)
                                                .setSize(15)
                                        )
                                .build()).get();

                response.getPlayersList()
                        .stream()
                        .map(McPlayer::getCurrentUsername)
                        .forEach(builder::suggest);

                return builder.build();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).exceptionally(throwable -> {
            LOGGER.error("Failed to retrieve player suggestions", throwable);
            return builder.build();
        });
    }
}
