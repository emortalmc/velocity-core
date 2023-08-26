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
import dev.emortal.velocity.utils.DurationFormatter;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

final class OtherPlaytimeCommand implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelfPlaytimeCommand.class);

    private static final String PLAYTIME_OTHER_MESSAGE = "<light_purple><name>'s playtime is <playtime>.";

    private final McPlayerService playerService;

    OtherPlaytimeCommand(@NotNull McPlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player player = (Player) source;
        String targetName = arguments.getArgument("username", String.class);

        McPlayer targetPlayer;
        try {
            targetPlayer = this.playerService.getPlayerByUsername(targetName);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get playtime for player {}", targetName, exception);
            player.sendMessage(Component.text("Failed to get playtime.", NamedTextColor.RED));
            return;
        }

        if (targetPlayer == null) {
            LOGGER.error("Player {} who requested their own playtime could not be found!", targetName);
            player.sendMessage(Component.text("You do not exist. Please report this to an administrator.", NamedTextColor.RED));
            return;
        }

        LoginSession currentSession = targetPlayer.hasCurrentSession() ? targetPlayer.getCurrentSession() : null;

        Duration currentSessionDuration = currentSession == null ? Duration.ZERO
                : Duration.between(ProtoTimestampConverter.fromProto(currentSession.getLoginTime()), Instant.now());
        Duration totalDuration = currentSessionDuration.plus(ProtoDurationConverter.fromProto(targetPlayer.getHistoricPlayTime()));

        String correctedUsername = targetPlayer.getCurrentUsername();
        String playtime = DurationFormatter.formatBigToSmall(totalDuration);
        Component message = MiniMessage.miniMessage().deserialize(PLAYTIME_OTHER_MESSAGE,
                Placeholder.unparsed("playtime", playtime), Placeholder.unparsed("name", correctedUsername));

        player.sendMessage(message);
    }
}
