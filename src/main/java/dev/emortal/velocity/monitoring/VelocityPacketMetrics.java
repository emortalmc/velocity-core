package dev.emortal.velocity.monitoring;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityPacketMetrics implements MeterBinder {
    private MeterRegistry meterRegistry;

    private final Map<StateRegistry, Map<Integer, Counter>> sentPacketCounters = new EnumMap<>(StateRegistry.class) {{
        for (StateRegistry stateRegistry : StateRegistry.values()) {
            put(stateRegistry, new ConcurrentHashMap<>());
        }
    }};
    private final Map<StateRegistry, Map<Integer, Counter>> receivedPacketCounters = new EnumMap<>(StateRegistry.class) {{
        for (StateRegistry stateRegistry : StateRegistry.values()) {
            put(stateRegistry, new ConcurrentHashMap<>());
        }
    }};


    @Override
    public void bindTo(@NotNull MeterRegistry registry) {
        this.meterRegistry = registry;
    }

    @Subscribe
    private void onLogin(@NotNull PostLoginEvent event) {
        ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
        Channel channel = player.getConnection().getChannel();

        channel.pipeline().addBefore("minecraft-encoder", "packet-out-counter", new PacketCounter(player, ProtocolUtils.Direction.CLIENTBOUND));
        channel.pipeline().addBefore("minecraft-decoder", "packet-in-counter", new PacketCounter(player, ProtocolUtils.Direction.SERVERBOUND));
    }

    private final class PacketCounter extends ChannelDuplexHandler {
        private final @NotNull ConnectedPlayer player;
        private final @NotNull ProtocolUtils.Direction direction;

        private PacketCounter(@NotNull ConnectedPlayer player, @NotNull ProtocolUtils.Direction direction) {
            this.player = player;
            this.direction = direction;
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
            if (this.direction == ProtocolUtils.Direction.SERVERBOUND)
                this.handlePacket(VelocityPacketMetrics.this.receivedPacketCounters, ((ByteBuf) msg).copy());

            super.channelRead(ctx, msg);
        }

        @Override
        public void write(@NotNull ChannelHandlerContext ctx, @NotNull Object msg, @NotNull ChannelPromise promise) throws Exception {
            if (this.direction == ProtocolUtils.Direction.CLIENTBOUND)
                this.handlePacket(VelocityPacketMetrics.this.sentPacketCounters, ((ByteBuf) msg).copy());

            super.write(ctx, msg, promise);
        }

        private void handlePacket(@NotNull Map<StateRegistry, Map<Integer, Counter>> baseMap, @NotNull ByteBuf buf) {
            // readVarIntSafely will reutn Integer.MIN_VALUE on an invalid buffer
            int packetId = ProtocolUtils.readVarIntSafely(buf);

            StateRegistry connState = this.getConnectionState(this.player);
            Map<Integer, Counter> packetMap = baseMap.get(connState);

            Counter counter = packetMap.get(packetId);
            if (counter == null) {
                counter = Counter.builder("velocity.packets")
                        .tag("direction", this.direction == ProtocolUtils.Direction.CLIENTBOUND ? "clientbound" : "serverbound")
                        .tag("id", String.valueOf(packetId))
                        .tag("state", this.player.getConnection().getState().toString())
                        .description("The amount of packets sent by the server")
                        .register(VelocityPacketMetrics.this.meterRegistry);

                packetMap.put(packetId, counter);
            }

            counter.increment();
        }

        private StateRegistry getConnectionState(@NotNull ConnectedPlayer player) {
            return player.getConnection().getState();
        }
    }
}
