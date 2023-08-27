package dev.emortal.velocity.player.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.mcplayer.McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod;
import dev.emortal.api.model.mcplayer.LoginSession;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.utils.ProtoDurationConverter;
import dev.emortal.api.utils.ProtoTimestampConverter;
import dev.emortal.velocity.command.CommandConditions;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.SessionCache;
import dev.emortal.velocity.player.UsernameSuggestions;
import dev.emortal.velocity.utils.DurationFormatter;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public final class PlaytimeCommand extends EmortalCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaytimeCommand.class);

    private final McPlayerService playerService;
    private final UsernameSuggestions usernameSuggestions;
    private final SessionCache sessionCache;

    public PlaytimeCommand(@NotNull McPlayerService playerService, @NotNull SessionCache sessionCache,
                           @NotNull UsernameSuggestions usernameSuggestions) {
        super("playtime");

        this.playerService = playerService;
        this.sessionCache = sessionCache;
        this.usernameSuggestions = usernameSuggestions;

        super.setCondition(CommandConditions.playerOnly());
        super.setDefaultExecutor(this::executeForSelf);

        var usernameArgument = argument("username", StringArgumentType.word(), this.usernameSuggestions.command(FilterMethod.NONE));
        super.addSyntax(this::executeForOther, usernameArgument);
    }

    private void executeForSelf(@NotNull CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();

        McPlayer mcPlayer;
        try {
            mcPlayer = this.playerService.getPlayerById(player.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get player data for '{}'", player.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        SessionCache.CachedSession currentSession = this.sessionCache.get(player.getUniqueId());
        if (currentSession == null) {
            LOGGER.error("Failed to get current session for '{}'", player.getUsername());
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        Duration currentSessionDuration = Duration.between(currentSession.loginTime(), Instant.now());
        Duration totalDuration = ProtoDurationConverter.fromProto(mcPlayer.getHistoricPlayTime()).plus(currentSessionDuration);

        String playtime = DurationFormatter.formatBigToSmall(totalDuration);
        ChatMessages.YOUR_PLAYTIME.send(player, Component.text(playtime));
    }

    private void executeForOther(@NotNull CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();
        String targetName = StringArgumentType.getString(context, "username");

        McPlayer targetPlayer;
        try {
            targetPlayer = PlaytimeCommand.this.playerService.getPlayerByUsername(targetName);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetName, exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        if (targetPlayer == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(player, Component.text(targetName));
            return;
        }

        LoginSession currentSession = targetPlayer.hasCurrentSession() ? targetPlayer.getCurrentSession() : null;

        Duration currentSessionDuration = currentSession == null ? Duration.ZERO
                : Duration.between(ProtoTimestampConverter.fromProto(currentSession.getLoginTime()), Instant.now());
        Duration totalDuration = currentSessionDuration.plus(ProtoDurationConverter.fromProto(targetPlayer.getHistoricPlayTime()));

        String correctedUsername = targetPlayer.getCurrentUsername();
        String playtime = DurationFormatter.formatBigToSmall(totalDuration);
        ChatMessages.OTHER_PLAYTIME.send(player, Component.text(correctedUsername), Component.text(playtime));
    }
}
