package dev.emortal.velocity.relationships.commands.block;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.DeleteBlockResult;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.command.CommandConditions;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.suggestions.UsernameSuggesterProvider;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class UnblockCommand extends EmortalCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(UnblockCommand.class);

    private static final Component USAGE = MINI_MESSAGE.deserialize("<red>Usage: /unblock <username>");

    private final McPlayerService mcPlayerService;
    private final RelationshipService relationshipService;

    public UnblockCommand(@NotNull McPlayerService mcPlayerService, @NotNull RelationshipService relationshipService,
                          @NotNull UsernameSuggesterProvider usernameSuggesters) {
        super("unblock");
        this.mcPlayerService = mcPlayerService;
        this.relationshipService = relationshipService;

        super.setCondition(CommandConditions.playerOnly());
        super.setDefaultExecutor(context -> context.getSource().sendMessage(USAGE));

        var usernameArgument = argument("username", StringArgumentType.string(), usernameSuggesters.all());
        super.addSyntax(this::execute, usernameArgument);
    }

    private void execute(@NotNull CommandContext<CommandSource> context) {
        Player sender = (Player) context.getSource();
        String targetUsername = StringArgumentType.getString(context, "username");

        McPlayer target;
        try {
            target = this.mcPlayerService.getPlayerByUsername(targetUsername);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(sender);
            return;
        }

        if (target == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(sender);
            return;
        }

        UUID targetId = UUID.fromString(target.getId());
        if (targetId.equals(sender.getUniqueId())) {
            ChatMessages.ERROR_CANNOT_UNBLOCK_SELF.send(sender);
            return;
        }

        DeleteBlockResult result;
        try {
            result = this.relationshipService.unblock(sender.getUniqueId(), targetId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to unblock '{}' for '{}'", targetUsername, sender.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(sender);
            return;
        }

        switch (result) {
            case SUCCESS -> ChatMessages.YOU_UNBLOCKED.send(sender, Component.text(target.getCurrentUsername()));
            case NOT_BLOCKED -> ChatMessages.ERROR_NOT_BLOCKED.send(sender);
        }
    }
}
