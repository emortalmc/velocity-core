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
import dev.emortal.velocity.player.SessionCache;
import dev.emortal.velocity.player.UsernameSuggestions;
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

public final class PlaytimeCommand extends EmortalCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaytimeCommand.class);

    private static final String PLAYTIME_SELF_MESSAGE = "<light_purple>Your playtime is <playtime>.";
    private static final String PLAYTIME_OTHER_MESSAGE = "<light_purple><name>'s playtime is <playtime>.";

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
            LOGGER.error("Failed to get playtime for player {}", player.getUsername(), exception);
            player.sendMessage(Component.text("Failed to get playtime."));
            return;
        }

        SessionCache.CachedSession currentSession = this.sessionCache.get(player.getUniqueId());
        if (currentSession == null) {
            LOGGER.error("The session for {} who requested their own playtime could not be found!", player.getUniqueId());
            player.sendMessage(Component.text("You do not exist. Please report this to an administrator."));
            return;
        }

        Duration currentSessionDuration = Duration.between(currentSession.loginTime(), Instant.now());
        Duration totalDuration = ProtoDurationConverter.fromProto(mcPlayer.getHistoricPlayTime()).plus(currentSessionDuration);

        String playtime = DurationFormatter.formatBigToSmall(totalDuration);
        Component message = MINI_MESSAGE.deserialize(PLAYTIME_SELF_MESSAGE, Placeholder.unparsed("playtime", playtime));
        player.sendMessage(message);
    }

    private void executeForOther(@NotNull CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();
        String targetName = StringArgumentType.getString(context, "username");

        McPlayer targetPlayer;
        try {
            targetPlayer = PlaytimeCommand.this.playerService.getPlayerByUsername(targetName);
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
        Component message = MINI_MESSAGE.deserialize(PLAYTIME_OTHER_MESSAGE,
                Placeholder.unparsed("playtime", playtime), Placeholder.unparsed("name", correctedUsername));

        player.sendMessage(message);
    }
}
