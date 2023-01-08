package dev.emortal.velocity.general;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.McPlayerGrpc;
import dev.emortal.api.service.McPlayerProto;
import dev.emortal.api.utils.GrpcStubCollection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class UsernameSuggestions {
    private final McPlayerGrpc.McPlayerFutureStub playerService;

    public UsernameSuggestions() {
        this.playerService = GrpcStubCollection.getPlayerService().orElse(null);
    }

    public CompletableFuture<Suggestions> command(
            CommandContext<CommandSource> context, SuggestionsBuilder builder,
            McPlayerProto.McPlayerSearchRequest.FilterMethod filterMethod) {

        if (!(context.getSource() instanceof Player player)) return CompletableFuture.completedFuture(builder.build());

        String currentInput = builder.getRemainingLowerCase();
        if (currentInput.length() < 3) return CompletableFuture.completedFuture(builder.build());

        return CompletableFuture.supplyAsync(() -> {
            try {
                McPlayerProto.PlayerSearchResponse response = this.playerService.searchPlayersByUsername(
                        McPlayerProto.McPlayerSearchRequest.newBuilder()
                                .setIssuerId(player.getUniqueId().toString())
                                .setSearchUsername(currentInput)
                                .setPage(0)
                                .setPageSize(15)
                                .setFilterMethod(filterMethod)
                                .build()).get();

                response.getPlayersList()
                        .stream()
                        .map(McPlayerProto.PlayerResponse::getCurrentUsername)
                        .forEach(builder::suggest);

                return builder.build();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
