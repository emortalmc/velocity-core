package dev.emortal.velocity.player.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.mcplayer.LoginSession;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.utils.ProtoDurationConverter;
import dev.emortal.api.utils.ProtoTimestampConverter;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.utils.DurationFormatter;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

final class OtherPlaytimeCommand implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(OtherPlaytimeCommand.class);

    private final @NotNull McPlayerService playerService;

    OtherPlaytimeCommand(@NotNull McPlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player player = (Player) source;
        String targetName = arguments.getArgument("username", String.class);

        McPlayer target;
        try {
            target = this.playerService.getPlayerByUsername(targetName);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetName, exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        if (target == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(player, targetName);
            return;
        }

        LoginSession currentSession = target.hasCurrentSession() ? target.getCurrentSession() : null;

        Duration currentSessionDuration = currentSession == null ? Duration.ZERO
                : Duration.between(ProtoTimestampConverter.fromProto(currentSession.getLoginTime()), Instant.now());
        Duration totalDuration = currentSessionDuration.plus(ProtoDurationConverter.fromProto(target.getHistoricPlayTime()));

        String correctedUsername = target.getCurrentUsername();
        String playtime = DurationFormatter.formatBigToSmall(totalDuration);
        ChatMessages.OTHER_PLAYTIME.send(player, correctedUsername, playtime);
    }
}
