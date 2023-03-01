package dev.emortal.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.agonessdk.AgonesUtils;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.cache.SessionCache;
import dev.emortal.velocity.relationships.FriendCache;
import dev.emortal.velocity.relationships.commands.block.BlockCommand;
import dev.emortal.velocity.relationships.commands.friend.FriendCommand;
import dev.emortal.velocity.relationships.listeners.FriendRabbitMqListener;
import dev.emortal.velocity.general.UsernameSuggestions;
import dev.emortal.velocity.general.commands.PlaytimeCommand;
import dev.emortal.velocity.grpc.stub.GrpcStubManager;
import dev.emortal.velocity.listener.AgonesListener;
import dev.emortal.velocity.listener.LobbySelectorListener;
import dev.emortal.velocity.listener.McPlayerListener;
import dev.emortal.velocity.listener.ServerChangeNotificationListener;
import dev.emortal.velocity.permissions.PermissionCache;
import dev.emortal.velocity.permissions.commands.PermissionCommand;
import dev.emortal.velocity.permissions.listener.PermissionCheckListener;
import dev.emortal.velocity.privatemessages.LastMessageCache;
import dev.emortal.velocity.privatemessages.PrivateMessageListener;
import dev.emortal.velocity.privatemessages.commands.MessageCommand;
import dev.emortal.velocity.rabbitmq.RabbitMqCore;
import dev.emortal.velocity.serverlist.ServerPingListener;
import dev.emortal.velocity.tablist.TabList;
import dev.emortal.velocity.utils.ReflectionUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class CorePlugin {
    private static final Map<Integer, AtomicLong> OUTGOING_PACKET_COUNTER = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(CorePlugin.class);

    public static final boolean DEBUG_PACKETS = Boolean.getBoolean("VELOCITY_DEBUG_PACKETS");
    public static final String SERVER_ID = System.getenv("HOSTNAME");
    public static final boolean DEV_ENVIRONMENT = System.getenv("AGONES_SDK_GRPC_PORT") == null;

    private final ProxyServer proxy;

    private final GrpcStubManager stubManager = new GrpcStubManager();

    private final UsernameSuggestions usernameSuggestions = new UsernameSuggestions();

    private final RabbitMqCore rabbitMqCore = new RabbitMqCore();

    private final FriendCache friendCache = new FriendCache();
    private final SessionCache sessionCache = new SessionCache();
    private final LastMessageCache lastMessageCache = new LastMessageCache();
    private PermissionCache permissionCache;

    @Inject
    public CorePlugin(ProxyServer server) {
        this.proxy = server;

        PlayerResolver.setPlatformUsernameResolver(username -> this.proxy.getPlayer(username).map(player -> new PlayerResolver.CachedMcPlayer(player.getUniqueId(), player.getUsername())).orElse(null));
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        if (this.stubManager.getAgonesService() != null) {
            this.proxy.getEventManager().register(this, new AgonesListener(this.stubManager.getAgonesService(),
                    this.stubManager.getStandardAgonesService(), this.stubManager.getAlphaAgonesService())
            );
        } else {
            LOGGER.warn("Agones SDK is not enabled. This is only intended for development purposes.");
        }

        this.proxy.getEventManager().register(this, this.sessionCache);

        new ServerChangeNotificationListener(this.proxy, this.rabbitMqCore); // Listens for RabbitMQ ProxyServerChangeMessage messages
        this.permissionCache = new PermissionCache(this.stubManager);

        // rabbitmq
        this.proxy.getEventManager().register(this, this.rabbitMqCore);

        // friends
        new FriendRabbitMqListener(this.rabbitMqCore, this.proxy, this.friendCache);
        this.proxy.getEventManager().register(this, this.friendCache);

        // private messages
        this.proxy.getEventManager().register(this, new PrivateMessageListener(this.rabbitMqCore, this.proxy, this.lastMessageCache));
        this.proxy.getEventManager().register(this, this.lastMessageCache);

        // permissions
        this.proxy.getEventManager().register(this, this.permissionCache);
        this.proxy.getEventManager().register(this, new PermissionCheckListener(this.permissionCache));

        // generic
        this.proxy.getEventManager().register(this, new LobbySelectorListener(this.proxy));
        this.proxy.getEventManager().register(this, new McPlayerListener(this.sessionCache));

        // server list
        this.proxy.getEventManager().register(this, new ServerPingListener());

        // tablist
        this.proxy.getEventManager().register(this, new TabList(this, this.proxy));

        new FriendCommand(this.proxy, this.usernameSuggestions, this.friendCache);
        new BlockCommand(this.proxy, this.usernameSuggestions);

        new PermissionCommand(this.proxy, this.permissionCache, this.usernameSuggestions);

        new MessageCommand(this.proxy, this.usernameSuggestions, this.lastMessageCache);

        // generic
        new PlaytimeCommand(this.proxy, this.sessionCache, this.usernameSuggestions);

        new ServerCleanupTask(this, this.proxy);

        if (DEBUG_PACKETS) {
            this.proxy.getScheduler().buildTask(this, () -> {
                List<PacketStat> packetStats = OUTGOING_PACKET_COUNTER.entrySet().stream()
                        .map(entry -> new PacketStat(entry.getKey(), entry.getValue().get()))
                        .sorted()
                        .toList();

                StringJoiner joiner = new StringJoiner("\n");
                joiner.add("Packet Stats:");

                for (int i = 0; i < 10 && i < packetStats.size(); i++) {
                    PacketStat packetStat = packetStats.get(i);
                    joiner.add("%s) %s - %s".formatted(i + 1, packetStat.id, packetStat.count));
                }
                System.out.println(joiner);
            }).repeat(10, TimeUnit.SECONDS).schedule();
        }
    }

    private record PacketStat(int id, long count) implements Comparable<PacketStat> {
        @Override
        public int compareTo(PacketStat o) {
            return Long.compare(o.count, this.count);
        }
    }

    @Subscribe
    public void onLogin(PostLoginEvent event) {
        if (!DEBUG_PACKETS) return;

        Player player = event.getPlayer();
        Object minecraftConnection = ReflectionUtils.get(player, player.getClass(), "connection", Object.class);
        Channel channel = ReflectionUtils.get(minecraftConnection, minecraftConnection.getClass(), "channel", Channel.class);

        System.out.println("Registering custom channel thingy");
        channel.eventLoop().submit(() -> {
            channel.pipeline().addBefore("minecraft-encoder", "packet-counter", new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    super.channelRead(ctx, msg);
                }

                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    ByteBuf byteBuf = ((ByteBuf) msg).copy();
                    int packetId = readVarInt(byteBuf);
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

    public int readVarInt(ByteBuf buf) {
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
    public void onProxyShutdown(ProxyShutdownEvent event) {
        this.rabbitMqCore.shutdown();
        AgonesUtils.shutdownHealthTask();
    }

    public ProxyServer getProxy() {
        return this.proxy;
    }
}
