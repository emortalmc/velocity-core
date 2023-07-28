package dev.emortal.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.agonessdk.AgonesUtils;
import dev.emortal.api.service.matchmaker.MatchmakerService;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.messagehandler.MessageService;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.cache.SessionCache;
import dev.emortal.velocity.general.UsernameSuggestions;
import dev.emortal.velocity.general.commands.LobbyCommand;
import dev.emortal.velocity.general.commands.PlaytimeCommand;
import dev.emortal.velocity.grpc.stub.GrpcStubManager;
import dev.emortal.velocity.listener.AgonesListener;
import dev.emortal.velocity.listener.FriendConnectionListener;
import dev.emortal.velocity.listener.LobbySelectorListener;
import dev.emortal.velocity.listener.LunarKicker;
import dev.emortal.velocity.listener.McPlayerListener;
import dev.emortal.velocity.listener.ServerChangeNotificationListener;
import dev.emortal.velocity.liveconfig.LiveConfigProvider;
import dev.emortal.velocity.messaging.MessagingCore;
import dev.emortal.velocity.party.PartyCache;
import dev.emortal.velocity.party.commands.PartyCommand;
import dev.emortal.velocity.permissions.PermissionCache;
import dev.emortal.velocity.permissions.commands.PermissionCommand;
import dev.emortal.velocity.permissions.listener.PermissionCheckListener;
import dev.emortal.velocity.permissions.listener.PermissionUpdateListener;
import dev.emortal.velocity.privatemessages.LastMessageCache;
import dev.emortal.velocity.privatemessages.PrivateMessageListener;
import dev.emortal.velocity.privatemessages.commands.MessageCommand;
import dev.emortal.velocity.relationships.FriendCache;
import dev.emortal.velocity.relationships.commands.block.BlockCommand;
import dev.emortal.velocity.relationships.commands.friend.FriendCommand;
import dev.emortal.velocity.relationships.listeners.FriendListener;
import dev.emortal.velocity.resourcepack.ResourcePackForcer;
import dev.emortal.velocity.serverlist.ServerPingListener;
import dev.emortal.velocity.tablist.TabList;
import dev.emortal.velocity.utils.ReflectionUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Plugin(
        id = "core",
        name = "Core"
)
public final class CorePlugin {
    private static final Map<Integer, AtomicLong> OUTGOING_PACKET_COUNTER = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(CorePlugin.class);

    public static final boolean DEBUG_PACKETS = Boolean.getBoolean("VELOCITY_DEBUG_PACKETS");
    public static final String SERVER_ID = System.getenv("HOSTNAME");
    public static final boolean DEV_ENVIRONMENT = System.getenv("AGONES_SDK_GRPC_PORT") == null;

    private final ProxyServer proxy;

    private final GrpcStubManager stubManager = new GrpcStubManager();

    private MessagingCore messagingCore;

    private final SessionCache sessionCache = new SessionCache();
    private final LiveConfigProvider liveConfigProvider;

    @Inject
    public CorePlugin(@NotNull ProxyServer server) {
        this.proxy = server;

        PyroscopeHandler.register();

        PlayerResolver.setPlatformUsernameResolver(username -> this.proxy.getPlayer(username).map(player -> new PlayerResolver.CachedMcPlayer(player.getUniqueId(), player.getUsername(), true)).orElse(null));

        this.liveConfigProvider = new LiveConfigProvider();
    }

    @Subscribe
    public void onProxyInitialize(@NotNull ProxyInitializeEvent event) {
        EventManager eventManager = this.proxy.getEventManager();

        if (this.stubManager.getAgonesService() != null) {
            eventManager.register(this, new AgonesListener(this.stubManager));
        } else {
            LOGGER.warn("Agones SDK is not enabled. This is only intended for development purposes.");
        }

        // Late init because we need the proxy
        this.messagingCore = new MessagingCore();

        eventManager.register(this, this.sessionCache);

        new FriendConnectionListener(this.proxy, this.messagingCore); // Listens for FriendConnectionMessage messages

        new ServerChangeNotificationListener(this.proxy, this.messagingCore); // Listens for ProxyServerChangeMessage messages

        // messaging core
        eventManager.register(this, this.messagingCore);

        McPlayerService playerService = GrpcStubCollection.getPlayerService().orElse(null);
        UsernameSuggestions usernameSuggestions;
        if (playerService != null) {
            usernameSuggestions = new UsernameSuggestions(playerService);

            // relationships
            this.registerRelationshipServices(eventManager, playerService, this.messagingCore, usernameSuggestions);

            // mc player listener
            eventManager.register(this, new McPlayerListener(playerService, this.sessionCache));

            // playtime
            new PlaytimeCommand(this.proxy, playerService, this.sessionCache, usernameSuggestions);
        } else {
            usernameSuggestions = new UsernameSuggestions(null);
            LOGGER.error("MC player service unavailable.");
        }

        MatchmakerService matchmaker = GrpcStubCollection.getMatchmakerService().orElse(null);
        if (matchmaker != null) {
            // lobby selector
            eventManager.register(this, new LobbySelectorListener(this.proxy, matchmaker, this.messagingCore));

            // lobby command
            new LobbyCommand(this.proxy, matchmaker);
        } else {
            LOGGER.error("Matchmaker service unavailable.");
        }

        // private messages
        this.registerMessageServices(eventManager, usernameSuggestions);

        // permissions
        this.registerPermissionServices(eventManager, usernameSuggestions);

        // server list
        eventManager.register(this, new ServerPingListener());

        // tablist
        eventManager.register(this, new TabList(this, this.proxy));

        // resource pack
        eventManager.register(this, new ResourcePackForcer(this.proxy));

        // fuck lunar
        eventManager.register(this, new LunarKicker());

        // party
        this.registerPartyServices(usernameSuggestions);

        // permission
        this.registerPermissionServices(eventManager, usernameSuggestions);

        // server cleanup
        eventManager.register(this, new ServerCleanupTask(this.proxy));

        if (DEBUG_PACKETS) {
            this.proxy.getScheduler().buildTask(this, this::registerDebugStatistics).repeat(10, TimeUnit.SECONDS).schedule();
        }
    }

    private void registerPartyServices(@NotNull UsernameSuggestions usernameSuggestions) {
        PartyService service = GrpcStubCollection.getPartyService().orElse(null);
        if (service == null) {
            LOGGER.error("Party service unavailable. Party command will not be registered.");
            return;
        }

        PartyCache partyCache = new PartyCache(this.proxy, service, this.messagingCore);
        new PartyCommand(this.proxy, service, usernameSuggestions, partyCache);
    }

    private void registerRelationshipServices(@NotNull EventManager eventManager, @NotNull McPlayerService mcPlayerService,
                                              @NotNull MessagingCore messagingCore, @NotNull UsernameSuggestions usernameSuggestions) {
        RelationshipService service = GrpcStubCollection.getRelationshipService().orElse(null);
        if (service == null) {
            LOGGER.error("Relationship service unavailable. Friend and block commands will not be registered.");
            return;
        }

        FriendCache cache = new FriendCache(service);

        new FriendListener(this.proxy, messagingCore, cache);
        eventManager.register(this, cache);

        new FriendCommand(this.proxy, mcPlayerService, service, usernameSuggestions, cache, this.liveConfigProvider.getGameModes());
        new BlockCommand(this.proxy, mcPlayerService, service, usernameSuggestions);
    }

    private void registerPermissionServices(@NotNull EventManager eventManager, @NotNull UsernameSuggestions usernameSuggestions) {
        PermissionService service = GrpcStubCollection.getPermissionService().orElse(null);
        if (service == null) {
            LOGGER.error("Permission service unavailable. Permission command will not be registered.");
            return;
        }

        PermissionCache cache = new PermissionCache(service);

        eventManager.register(this, cache);
        eventManager.register(this, new PermissionCheckListener(cache));
        new PermissionUpdateListener(cache, this.messagingCore);

        new PermissionCommand(this.proxy, service, cache, usernameSuggestions);
    }

    private void registerMessageServices(@NotNull EventManager eventManager, @NotNull UsernameSuggestions usernameSuggestions) {
        LastMessageCache lastMessageCache = new LastMessageCache();

        eventManager.register(this, new PrivateMessageListener(this.proxy, this.messagingCore, lastMessageCache));
        eventManager.register(this, lastMessageCache);

        MessageService service = GrpcStubCollection.getMessageHandlerService().orElse(null);
        if (service == null) {
            LOGGER.error("Message service unavailable. Message command will not be registered.");
            return;
        }

        new MessageCommand(this.proxy, service, usernameSuggestions, lastMessageCache);
    }

    private void registerDebugStatistics() {
        List<PacketStat> packetStats = OUTGOING_PACKET_COUNTER.entrySet().stream()
                .map(entry -> new PacketStat(entry.getKey(), entry.getValue().get()))
                .sorted()
                .toList();

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Packet Stats:");

        for (PacketStat(int id, long count) : packetStats) {
            joiner.add('\t' + id + " - " + count);
        }

        System.out.println(joiner);
    }

    private record PacketStat(int id, long count) implements Comparable<PacketStat> {

        @Override
        public int compareTo(@NotNull PacketStat o) {
            return Long.compare(o.count, this.count);
        }
    }

    @Subscribe
    public void onLogin(@NotNull PostLoginEvent event) {
        if (!DEBUG_PACKETS) return;

        Player player = event.getPlayer();
        Object minecraftConnection = ReflectionUtils.get(player, player.getClass(), "connection", Object.class);
        Channel channel = ReflectionUtils.get(minecraftConnection, minecraftConnection.getClass(), "channel", Channel.class);

        System.out.println("Registering custom channel thingy");
        channel.eventLoop().submit(() -> {
            channel.pipeline().addBefore("minecraft-encoder", "packet-counter", new ChannelDuplexHandler() {
                @Override
                public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
                    super.channelRead(ctx, msg);
                }

                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    ByteBuf byteBuf = ((ByteBuf) msg).copy();
                    int packetId = CorePlugin.this.readVarInt(byteBuf);
                    OUTGOING_PACKET_COUNTER.computeIfPresent(packetId, (id, count) -> {
                        count.incrementAndGet();
                        return count;
                    });
                    OUTGOING_PACKET_COUNTER.computeIfAbsent(packetId, id -> new AtomicLong(1));
                    super.write(ctx, msg, promise);
                }
            });
        });
    }

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    public int readVarInt(@NotNull ByteBuf buf) {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = buf.readByte();
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }

        return value;
    }

    @Subscribe
    public void onProxyShutdown(@NotNull ProxyShutdownEvent event) {
        this.messagingCore.shutdown();
        AgonesUtils.shutdownHealthTask();

        try {
            this.liveConfigProvider.close();
        } catch (IOException ignored) {
        }
    }

    public @NotNull ProxyServer getProxy() {
        return this.proxy;
    }
}
