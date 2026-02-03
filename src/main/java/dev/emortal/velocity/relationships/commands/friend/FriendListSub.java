package dev.emortal.velocity.relationships.commands.friend;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.liveconfigparser.configs.ConfigProvider;
import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeConfig;
import dev.emortal.api.model.mcplayer.McPlayer;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.utils.ProtoTimestampConverter;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.relationships.FriendCache;
import dev.emortal.velocity.utils.DurationFormatter;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class FriendListSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendListSub.class);

    private static final Component MESSAGE_FOOTER = Component.text("----------------------------", NamedTextColor.LIGHT_PURPLE);

    private final @NotNull McPlayerService playerService;
    private final @NotNull FriendCache friendCache;
    private final @Nullable ConfigProvider<GameModeConfig> gameModes;

    FriendListSub(@NotNull McPlayerService playerService, @NotNull FriendCache friendCache, @Nullable ConfigProvider<GameModeConfig> gameModes) {
        this.playerService = playerService;
        this.friendCache = friendCache;
        this.gameModes = gameModes;

        if (gameModes == null) LOGGER.warn("GameModeCollection is null. Friend statuses will not be displayed.");
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player player = (Player) source;

        List<FriendCache.CachedFriend> friends = this.friendCache.get(player.getUniqueId());
        int maxPage = (int) Math.ceil(friends.size() / 8.0);

        if (maxPage == 0) {
            ChatMessages.ERROR_NO_FRIENDS.send(player);
            return;
        }

        int page = arguments.hasArgument("page") ? Math.min(arguments.getArgument("page", Integer.class), maxPage) : 1;

        List<FriendStatus> statuses = this.retrieveStatuses(friends);
        Collections.sort(statuses);

        List<FriendStatus> pageFriends = statuses.stream()
                .skip((page - 1) * 8L)
                .limit(8)
                .toList();
        ChatMessages.FRIEND_LIST.send(player, page, maxPage, this.createMessageContent(pageFriends));
    }

    private @NotNull Component createMessageContent(@NotNull List<FriendStatus> statuses) {
        TextComponent.Builder result = Component.text();

        for (FriendStatus status : statuses) {
            ChatMessages.Args2<String, String> line = status.online() ? ChatMessages.FRIEND_LIST_ONLINE_LINE : ChatMessages.FRIEND_LIST_OFFLINE_LINE;

            String secondArgument;
            if (status.online()) {
                secondArgument = this.createActivityForServer(status.serverId());
            } else {
                secondArgument = DurationFormatter.formatShortFromInstant(status.lastSeen());
            }

            result.append(line.get(status.username(), secondArgument)).appendNewline();
        }

        return result.build();
    }

    private @NotNull List<FriendStatus> retrieveStatuses(@NotNull List<FriendCache.CachedFriend> friends) {
        Map<UUID, FriendStatus> statuses = new ConcurrentHashMap<>();
        for (FriendCache.CachedFriend friend : friends) {
            statuses.put(friend.playerId(), new FriendStatus(friend.playerId(), friend.friendsSince()));
        }

        List<UUID> playerIds = new ArrayList<>();
        for (FriendCache.CachedFriend friend : friends) {
            playerIds.add(friend.playerId());
        }

        List<McPlayer> players;
        try {
            players = this.playerService.getPlayersById(playerIds);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to resolve friends from IDs '{}'", playerIds, exception);
            return new ArrayList<>(statuses.values());
        }

        for (McPlayer player : players) {
            UUID uuid = UUID.fromString(player.getId());
            FriendStatus status = statuses.get(uuid);

            String serverId = player.hasCurrentServer() ? player.getCurrentServer().getServerId() : null;
            Instant lastSeen = ProtoTimestampConverter.fromProto(player.getLastOnline());
            FriendStatus newStatus = status.toFull(player.getCurrentUsername(), player.hasCurrentServer(), serverId, lastSeen);

            statuses.put(uuid, newStatus);
        }

        return new ArrayList<>(statuses.values());
    }

    private @NotNull String createActivityForServer(@NotNull String serverId) {
        String[] parts = serverId.split("-");
        String[] serverTypeIdParts = Arrays.copyOf(parts, parts.length - 2);
        String fleetId = String.join("-", serverTypeIdParts);

        GameModeConfig gameModeConfig = this.getGameModeConfig(fleetId);
        if (gameModeConfig != null) return gameModeConfig.activityNoun() + " " + gameModeConfig.friendlyName();

        LOGGER.warn("Could not find friendly name for fleet {}", fleetId);
        return fleetId;
    }

    private @Nullable GameModeConfig getGameModeConfig(@NotNull String fleetId) {
        if (this.gameModes == null) return null;

        GameModeConfig gameModeConfig = null;
        for (GameModeConfig config : this.gameModes.allConfigs()) {
            if (!config.fleetName().equals(fleetId)) continue;
            gameModeConfig = config;
            break;
        }

        return gameModeConfig;
    }

    private record FriendStatus(@NotNull UUID uuid, @NotNull Instant friendsSince, @Nullable String username, boolean online,
                                @Nullable String serverId, @Nullable Instant lastSeen) implements Comparable<FriendStatus> {

        FriendStatus(@NotNull UUID uuid, @NotNull Instant friendsSince) {
            this(uuid, friendsSince, null, false, null, null);
        }

        @NotNull FriendStatus toFull(@Nullable String username, boolean online, @Nullable String serverId, @Nullable Instant lastSeen) {
            return new FriendStatus(this.uuid, this.friendsSince, username, online, serverId, lastSeen);
        }

        public int compareTo(@NotNull FriendListSub.FriendStatus o) {
            if (this.online && !o.online) return -1;
            if (!this.online && o.online) return 1;
            if (!this.online) return o.lastSeen.compareTo(this.lastSeen); // both offline
            return this.username.compareTo(o.username);
        }
    }
}
