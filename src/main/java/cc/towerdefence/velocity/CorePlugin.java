package cc.towerdefence.velocity;

import cc.towerdefence.api.agonessdk.AgonesUtils;
import cc.towerdefence.api.utils.resolvers.PlayerResolver;
import cc.towerdefence.velocity.cache.SessionCache;
import cc.towerdefence.velocity.friends.FriendCache;
import cc.towerdefence.velocity.friends.commands.FriendCommand;
import cc.towerdefence.velocity.friends.listeners.FriendAddListener;
import cc.towerdefence.velocity.friends.listeners.FriendRemovalListener;
import cc.towerdefence.velocity.friends.listeners.FriendRequestListener;
import cc.towerdefence.velocity.general.ServerManager;
import cc.towerdefence.velocity.general.UsernameSuggestions;
import cc.towerdefence.velocity.general.commands.PlaytimeCommand;
import cc.towerdefence.velocity.grpc.service.GrpcServerContainer;
import cc.towerdefence.velocity.grpc.stub.GrpcStubManager;
import cc.towerdefence.velocity.listener.AgonesListener;
import cc.towerdefence.velocity.listener.LobbySelectorListener;
import cc.towerdefence.velocity.listener.McPlayerListener;
import cc.towerdefence.velocity.listener.OtpEventListener;
import cc.towerdefence.velocity.permissions.PermissionCache;
import cc.towerdefence.velocity.permissions.commands.PermissionCommand;
import cc.towerdefence.velocity.permissions.listener.PermissionCheckListener;
import cc.towerdefence.velocity.privatemessages.LastMessageCache;
import cc.towerdefence.velocity.privatemessages.PrivateMessageListener;
import cc.towerdefence.velocity.privatemessages.commands.MessageCommand;
import cc.towerdefence.velocity.rabbitmq.RabbitMqEventListener;
import cc.towerdefence.velocity.serverlist.ServerPingListener;
import cc.towerdefence.velocity.tablist.TabList;
import cc.towerdefence.velocity.utils.ReflectionUtils;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
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
    private final GrpcServerContainer grpcServerContainer;

    private final UsernameSuggestions usernameSuggestions = new UsernameSuggestions();

    private final RabbitMqEventListener rabbitMqEventListener = new RabbitMqEventListener();

    private final FriendCache friendCache = new FriendCache();
    private final SessionCache sessionCache = new SessionCache();
    private final LastMessageCache lastMessageCache = new LastMessageCache();
    private PermissionCache permissionCache;

    @Inject
    public CorePlugin(ProxyServer server) {
        this.proxy = server;
        this.grpcServerContainer = new GrpcServerContainer(this.proxy);

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

        ServerManager serverManager = new ServerManager(this, this.proxy);
        // OTP status affects a lot of functionality, so we need it to be loaded first
        OtpEventListener otpEventListener = new OtpEventListener(serverManager);
        this.permissionCache = new PermissionCache(this.stubManager, otpEventListener);

        // rabbitmq
        this.proxy.getEventManager().register(this, this.rabbitMqEventListener);

        // friends
        this.proxy.getEventManager().register(this, this.friendCache);
        this.proxy.getEventManager().register(this, new FriendAddListener(this.friendCache, this.proxy));
        this.proxy.getEventManager().register(this, new FriendRemovalListener(this.friendCache));
        this.proxy.getEventManager().register(this, new FriendRequestListener(this.proxy));

        // private messages
        this.proxy.getEventManager().register(this, new PrivateMessageListener(this.proxy, this.lastMessageCache));
        this.proxy.getEventManager().register(this, this.lastMessageCache);

        // permissions
        this.proxy.getEventManager().register(this, this.permissionCache);
        this.proxy.getEventManager().register(this, new PermissionCheckListener(this.permissionCache));

        // generic
        this.proxy.getEventManager().register(this, otpEventListener);
        this.proxy.getEventManager().register(this, new LobbySelectorListener(this.stubManager, this.proxy, otpEventListener));
        this.proxy.getEventManager().register(this, new McPlayerListener(this.sessionCache));

        // server list
        this.proxy.getEventManager().register(this, new ServerPingListener());

        // tablist
        this.proxy.getEventManager().register(this, new TabList(this, this.proxy));

        new FriendCommand(this.proxy, this.usernameSuggestions, this.friendCache);

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
        this.grpcServerContainer.stop();
        this.rabbitMqEventListener.shutdown();
        AgonesUtils.shutdownHealthTask();
    }

    public ProxyServer getProxy() {
        return this.proxy;
    }

    public GrpcStubManager getStubManager() {
        return this.stubManager;
    }
}
