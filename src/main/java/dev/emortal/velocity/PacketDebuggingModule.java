package dev.emortal.velocity;

import com.velocitypowered.api.scheduler.ScheduledTask;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ModuleData(name = "packet-debugging")
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

        this.task = super.adapters().scheduler().repeat(this::registerDebugStatistics, 10, TimeUnit.SECONDS);
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

        for (PacketStat packetStat : packetStats) {
            joiner.add('\t' + packetStat.id() + " - " + packetStat.count());
        }

        System.out.println(joiner);
    }

    private record PacketStat(int id, long count) implements Comparable<PacketStat> {

        @Override
        public int compareTo(@NotNull PacketStat o) {
            return Long.compare(o.count, this.count);
        }
    }
}
