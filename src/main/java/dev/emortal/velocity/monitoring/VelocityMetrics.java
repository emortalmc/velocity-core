package dev.emortal.velocity.monitoring;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jetbrains.annotations.NotNull;

public class VelocityMetrics implements MeterBinder {

    private final @NotNull PlayerProvider playerProvider;

    private Counter loginCounter;
    private Counter logoutCounter;

    public VelocityMetrics(@NotNull PlayerProvider playerProvider) {
        this.playerProvider = playerProvider;
    }

    @Override
    public void bindTo(@NotNull MeterRegistry registry) {
        Gauge.builder("velocity.players", this.playerProvider::playerCount)
                .description("The amount of players currently online")
                .register(registry);

        this.loginCounter = Counter.builder("velocity.player_flow")
                .description("The rate of players logging in and out")
                .tag("type", "login")
                .register(registry);

        this.logoutCounter = Counter.builder("velocity.player_flow")
                .description("The rate of players logging in and out")
                .tag("type", "logout")
                .register(registry);
    }

    @Subscribe
    public void onLogin(@NotNull PostLoginEvent event) {
        this.loginCounter.increment();
    }

    @Subscribe
    public void onLogout(@NotNull DisconnectEvent event) {
        this.logoutCounter.increment();
    }
}
