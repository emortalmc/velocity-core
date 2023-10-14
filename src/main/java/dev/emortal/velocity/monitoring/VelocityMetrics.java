package dev.emortal.velocity.monitoring;

import dev.emortal.velocity.adapter.player.PlayerProvider;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jetbrains.annotations.NotNull;

public class VelocityMetrics implements MeterBinder {

    private final @NotNull PlayerProvider playerProvider;

    public VelocityMetrics(@NotNull PlayerProvider playerProvider) {
        this.playerProvider = playerProvider;
    }

    @Override
    public void bindTo(@NotNull MeterRegistry registry) {
        Gauge.builder("velocity.players", this.playerProvider::playerCount)
                .description("The amount of players currently online")
                .register(registry);
    }
}
