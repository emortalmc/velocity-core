package dev.emortal.velocity.relationships.commands.block;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.service.relationship.DeleteBlockResult;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.resolver.CachedMcPlayer;
import dev.emortal.velocity.player.resolver.PlayerResolver;
import dev.emortal.velocity.player.suggestions.UsernameSuggesterProvider;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UnblockCommand extends EmortalCommand implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnblockCommand.class);

    private final @NotNull RelationshipService relationshipService;
    private final @NotNull PlayerResolver playerResolver;

    public UnblockCommand(@NotNull RelationshipService relationshipService, @NotNull PlayerResolver playerResolver,
                          @NotNull UsernameSuggesterProvider usernameSuggesters) {
        super("unblock");
        this.playerResolver = playerResolver;
        this.relationshipService = relationshipService;

        super.setPlayerOnly();
        super.setDefaultExecutor(context -> ChatMessages.UNBLOCK_USAGE.send(context.getSource()));

        var usernameArgument = argument("username", StringArgumentType.string(), usernameSuggesters.all());
        super.addSyntax(this, usernameArgument);
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player sender = (Player) source;
        String targetUsername = arguments.getArgument("username", String.class);

        CachedMcPlayer target;
        try {
            target = this.playerResolver.getPlayer(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(sender);
            return;
        }

        if (target == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(sender, targetUsername);
            return;
        }

        if (target.uuid().equals(sender.getUniqueId())) {
            ChatMessages.ERROR_CANNOT_UNBLOCK_SELF.send(sender);
            return;
        }

        DeleteBlockResult result;
        try {
            result = this.relationshipService.unblock(sender.getUniqueId(), target.uuid());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to unblock '{}' for '{}'", targetUsername, sender.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(sender);
            return;
        }

        switch (result) {
            case SUCCESS -> ChatMessages.YOU_UNBLOCKED.send(sender, target.username());
            case NOT_BLOCKED -> ChatMessages.ERROR_NOT_BLOCKED.send(sender);
        }
    }
}
