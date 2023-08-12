package dev.emortal.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import dev.emortal.api.modules.ModuleData;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ModuleData(name = "packet-debugging", required = false)
public final class PacketDebuggingModule extends VelocityModule {
    private static final boolean DEBUG_PACKETS = Boolean.getBoolean("VELOCITY_DEBUG_PACKETS");

    private final Map<Integer, AtomicLong> outgoingPacketCounter = new ConcurrentHashMap<>();
    private @Nullable ScheduledTask task;

    public PacketDebuggingModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        if (!DEBUG_PACKETS) return false;

        this.task = super.getProxy().getScheduler().buildTask(super.getEnvironment().plugin(), this::registerDebugStatistics)
                .repeat(10, TimeUnit.SECONDS)
                .schedule();
        super.registerEventListener(this);

        return true;
    }

    @Override
    public void onUnload() {
        if (this.task != null) this.task.cancel();
    }

    private void registerDebugStatistics() {
        List<PacketStat> packetStats = this.outgoingPacketCounter.entrySet().stream()
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

    @Subscribe
    private void onLogin(@NotNull PostLoginEvent event) {
        if (!DEBUG_PACKETS) return;

        ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
        Channel channel = player.getConnection().getChannel();

        channel.pipeline().addBefore("minecraft-encoder", "packet-counter", new PacketCounter());
    }

    private final class PacketCounter extends ChannelDuplexHandler {

        private static final int SEGMENT_BITS = 0x7F;
        private static final int CONTINUE_BIT = 0x80;

        @Override
        public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
            super.channelRead(ctx, msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            ByteBuf buf = ((ByteBuf) msg).copy();
            int packetId = readVarInt(buf);

            PacketDebuggingModule.this.outgoingPacketCounter.compute(packetId, (id, count) -> {
                if (count == null) return new AtomicLong(1);
                count.incrementAndGet();
                return count;
            });

            super.write(ctx, msg, promise);
        }

        private static int readVarInt(@NotNull ByteBuf buf) {
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
    }

    private record PacketStat(int id, long count) implements Comparable<PacketStat> {

        @Override
        public int compareTo(@NotNull PacketStat o) {
            return Long.compare(o.count, this.count);
        }
    }
}
