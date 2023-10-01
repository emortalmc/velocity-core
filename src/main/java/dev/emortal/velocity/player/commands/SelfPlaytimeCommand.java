package dev.emortal.velocity.player.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.utils.ProtoDurationConverter;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.SessionCache;
import dev.emortal.velocity.utils.DurationFormatter;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

final class SelfPlaytimeCommand implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelfPlaytimeCommand.class);

    private final @NotNull McPlayerService playerService;
    private final @NotNull SessionCache sessionCache;

    SelfPlaytimeCommand(@NotNull McPlayerService playerService, @NotNull SessionCache sessionCache) {
        this.playerService = playerService;
        this.sessionCache = sessionCache;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player player = (Player) source;

        McPlayer mcPlayer;
        try {
            mcPlayer = this.playerService.getPlayerById(player.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get playtime for player {}", player.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        SessionCache.CachedSession currentSession = this.sessionCache.get(player.getUniqueId());
        if (currentSession == null) {
            LOGGER.error("The session for {} who requested their own playtime could not be found!", player.getUniqueId());
            ChatMessages.GENERIC_ERROR.send(player);
            return;
        }

        Duration currentSessionDuration = Duration.between(currentSession.loginTime(), Instant.now());
        Duration totalDuration = ProtoDurationConverter.fromProto(mcPlayer.getHistoricPlayTime()).plus(currentSessionDuration);

        String playtime = DurationFormatter.formatBigToSmall(totalDuration);
        ChatMessages.YOUR_PLAYTIME.send(player, playtime);
    }
}
