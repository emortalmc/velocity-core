package dev.emortal.velocity.relationships.commands.block;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.mcplayer.McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.DeleteBlockResult;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.command.CommandConditions;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.player.UsernameSuggestions;
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
                          @NotNull UsernameSuggestions usernameSuggestions) {
        super("unblock");
        this.mcPlayerService = mcPlayerService;
        this.relationshipService = relationshipService;

        super.setCondition(CommandConditions.playerOnly());
        super.setDefaultExecutor(context -> context.getSource().sendMessage(USAGE));

        var usernameArgument = argument("username", StringArgumentType.string(), usernameSuggestions.command(FilterMethod.NONE));
        super.addSyntax(this::execute, usernameArgument);
    }

    private void execute(@NotNull CommandContext<CommandSource> context) {
        Player sender = (Player) context.getSource();
        String targetUsername = StringArgumentType.getString(context, "username");

        McPlayer target;
        try {
            target = this.mcPlayerService.getPlayerByUsername(targetUsername);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while trying to unblock the player", exception);
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to unblock the player"));
            return;
        }

        if (target == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Player not found"));
            return;
        }

        UUID targetId = UUID.fromString(target.getId());
        if (targetId.equals(sender.getUniqueId())) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>You can't unblock yourself"));
            return;
        }

        DeleteBlockResult result;
        try {
            result = this.relationshipService.unblock(sender.getUniqueId(), targetId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while trying to unblock the player", exception);
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>An error occurred while trying to unblock the player"));
            return;
        }

        var message = switch (result) {
            case SUCCESS -> Component.text(target.getCurrentUsername() + " has been unblocked");
            case NOT_BLOCKED -> MINI_MESSAGE.deserialize("<red>You have not blocked the player");
        };
        sender.sendMessage(message);
    }
}
