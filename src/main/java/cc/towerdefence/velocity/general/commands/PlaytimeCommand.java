package cc.towerdefence.velocity.general.commands;

import cc.towerdefence.api.service.McPlayerGrpc;
import cc.towerdefence.api.service.McPlayerProto;
import cc.towerdefence.api.utils.GrpcStubCollection;
import cc.towerdefence.api.utils.GrpcTimestampConverter;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import cc.towerdefence.velocity.cache.SessionCache;
import cc.towerdefence.velocity.general.UsernameSuggestions;
import cc.towerdefence.velocity.utils.CommandUtils;
import cc.towerdefence.velocity.utils.DurationFormatter;
import cc.towerdefence.velocity.utils.GrpcDurationConverter;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ForkJoinPool;

public class PlaytimeCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaytimeCommand.class);

    private static final String PLAYTIME_SELF_MESSAGE = "<light_purple>Your playtime is <playtime>.";
    private static final String PLAYTIME_OTHER_MESSAGE = "<light_purple><name>'s playtime is <playtime>.";

    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;
    private final UsernameSuggestions usernameSuggestions;
    private final SessionCache sessionCache;

    public PlaytimeCommand(ProxyServer proxy, SessionCache sessionCache, UsernameSuggestions usernameSuggestions) {
        this.mcPlayerService = GrpcStubCollection.getPlayerService().orElse(null);
        this.sessionCache = sessionCache;
        this.usernameSuggestions = usernameSuggestions;

        proxy.getCommandManager().register(this.createBrigadierCommand());
    }

    private int executePlayTimeSelf(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();

        ListenableFuture<McPlayerProto.PlayerResponse> playerResponseFuture = this.mcPlayerService.getPlayer(
                McPlayerProto.PlayerRequest.newBuilder().setPlayerId(player.getUniqueId().toString()).build()
        );

        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                playerResponse -> {
                    SessionCache.CachedSession currentSession = this.sessionCache.get(player.getUniqueId());
                    Duration currentSessionDuration = Duration.between(currentSession.loginTime(), Instant.now());
                    Duration totalDuration = GrpcDurationConverter.reverse(playerResponse.getPlayTime()).plus(currentSessionDuration);

                    String playtime = DurationFormatter.formatBigToSmall(totalDuration);
                    Component message = MINI_MESSAGE.deserialize(PLAYTIME_SELF_MESSAGE, Placeholder.unparsed("playtime", playtime));
                    player.sendMessage(message);
                },
                throwable -> {
                    LOGGER.error("Failed to get playtime for player {}", player.getUsername(), throwable);
                    player.sendMessage(Component.text("Failed to get playtime."));
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }

    private int executePlayTimeTarget(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();
        String targetName = StringArgumentType.getString(context, "username");

        ListenableFuture<McPlayerProto.PlayerResponse> playerResponseFuture = this.mcPlayerService.getPlayerByUsername(
                McPlayerProto.PlayerUsernameRequest.newBuilder().setUsername(targetName).build()
        );

        Futures.addCallback(playerResponseFuture, FunctionalFutureCallback.create(
                playerResponse -> {
                    McPlayerProto.PlayerSession currentSession = playerResponse.hasCurrentSession() ? playerResponse.getCurrentSession() : null;
                    Duration currentSessionDuration = currentSession == null ? Duration.ZERO
                            : Duration.between(GrpcTimestampConverter.reverse(currentSession.getLoginTime()), Instant.now());
                    Duration totalDuration = currentSessionDuration.plus(GrpcDurationConverter.reverse(playerResponse.getPlayTime()));

                    String correctedUsername = playerResponse.getCurrentUsername();
                    String playtime = DurationFormatter.formatBigToSmall(totalDuration);
                    Component message = MINI_MESSAGE.deserialize(PLAYTIME_OTHER_MESSAGE,
                            Placeholder.unparsed("playtime", playtime), Placeholder.unparsed("name", correctedUsername));

                    player.sendMessage(message);
                },
                throwable -> {
                    LOGGER.error("Failed to get playtime for player {}", targetName, throwable);
                    player.sendMessage(Component.text("Failed to get playtime."));
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }

    private BrigadierCommand createBrigadierCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("playtime")
                        .executes(this::executePlayTimeSelf)
                        .requires(CommandUtils.isPlayer())
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.word())
                                .suggests((context, builder) -> this.usernameSuggestions.command(context, builder, McPlayerProto.McPlayerSearchRequest.FilterMethod.NONE))
                                .executes(this::executePlayTimeTarget)
                        )
        );
    }
}
