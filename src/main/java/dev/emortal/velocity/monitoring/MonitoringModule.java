package dev.emortal.velocity.monitoring;

import com.sun.net.httpserver.HttpServer;
import dev.emortal.velocity.Environment;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusRenameFilter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Objects;

// Monitoring TODO
// - Metrics for all the kinds of caches we have and their sizes (e.g. parties, mc-player)

public class MonitoringModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringModule.class);

    private static final String FLEET_NAME = Objects.requireNonNullElse(System.getenv("FLEET_NAME"), "unknown");

    protected MonitoringModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        LOGGER.info("Starting monitoring with: [fleet={}, server={}]", FLEET_NAME, Environment.getHostname());

        var registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().meterFilter(new PrometheusRenameFilter()).commonTags("fleet", FLEET_NAME);

        if (Environment.isProduction()) {
            registry.config().commonTags("server", Environment.getHostname());
        }

        // Java
        new ClassLoaderMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);

        // Proc
        new ProcessorMetrics().bindTo(registry);

        // Custom
        new VelocityMetrics(this.adapters().playerProvider()).bindTo(registry);

        VelocityPacketMetrics packetMetrics = new VelocityPacketMetrics();
        packetMetrics.bindTo(registry);
        super.registerEventListener(packetMetrics);

        // Add the registry globally so that it can be used by other modules without having to pass it around
        Metrics.addRegistry(registry);

        try {
            LOGGER.info("Starting Prometheus HTTP server on port 8081");
            var server = HttpServer.create(new InetSocketAddress(8081), 0);

            server.createContext("/metrics", exchange -> {
                String response = registry.scrape();
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(response.getBytes());
                }
                exchange.close();
            });

            new Thread(server::start, "micrometer-http").start();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return true;
    }

    @Override
    public void onUnload() {

    }
}
