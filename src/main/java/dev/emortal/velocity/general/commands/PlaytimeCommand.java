package dev.emortal.velocity.general.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.grpc.mcplayer.McPlayerProto;
import dev.emortal.api.model.mcplayer.LoginSession;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.ProtoDurationConverter;
import dev.emortal.api.utils.ProtoTimestampConverter;
import dev.emortal.velocity.cache.SessionCache;
import dev.emortal.velocity.general.UsernameSuggestions;
import dev.emortal.velocity.utils.CommandUtils;
import dev.emortal.velocity.utils.DurationFormatter;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class PlaytimeCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaytimeCommand.class);

    private static final String PLAYTIME_SELF_MESSAGE = "<light_purple>Your playtime is <playtime>.";
    private static final String PLAYTIME_OTHER_MESSAGE = "<light_purple><name>'s playtime is <playtime>.";

    private final McPlayerService mcPlayerService;
    private final UsernameSuggestions usernameSuggestions;
    private final SessionCache sessionCache;

    public PlaytimeCommand(ProxyServer proxy, SessionCache sessionCache, UsernameSuggestions usernameSuggestions) {
        this.mcPlayerService = GrpcStubCollection.getPlayerService().orElse(null);
        this.sessionCache = sessionCache;
        this.usernameSuggestions = usernameSuggestions;

        proxy.getCommandManager().register(this.createBrigadierCommand());
    }

    private void executePlayTimeSelf(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();

        McPlayer mcPlayer;
        try {
            mcPlayer = this.mcPlayerService.getPlayerById(player.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get playtime for player {}", player.getUsername(), exception);
            player.sendMessage(Component.text("Failed to get playtime."));
            return;
        }

        SessionCache.CachedSession currentSession = this.sessionCache.get(player.getUniqueId());
        Duration currentSessionDuration = Duration.between(currentSession.loginTime(), Instant.now());
        Duration totalDuration = ProtoDurationConverter.fromProto(mcPlayer.getHistoricPlayTime()).plus(currentSessionDuration);

        String playtime = DurationFormatter.formatBigToSmall(totalDuration);
        Component message = MINI_MESSAGE.deserialize(PLAYTIME_SELF_MESSAGE, Placeholder.unparsed("playtime", playtime));
        player.sendMessage(message);
    }

    private void executePlayTimeTarget(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();
        String targetName = StringArgumentType.getString(context, "username");

        McPlayer targetPlayer;
        try {
            targetPlayer = this.mcPlayerService.getPlayerByUsername(targetName);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get playtime for player {}", targetName, exception);
            player.sendMessage(Component.text("Failed to get playtime."));
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

    private BrigadierCommand createBrigadierCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("playtime")
                        .executes(CommandUtils.executeAsync(this::executePlayTimeSelf))
                        .requires(CommandUtils.isPlayer())
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.word())
                                .suggests((context, builder) -> this.usernameSuggestions.command(context, builder, McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod.NONE))
                                .executes(CommandUtils.executeAsync(this::executePlayTimeTarget))
                        )
        );
    }
}
