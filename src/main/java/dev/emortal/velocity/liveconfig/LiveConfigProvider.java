package dev.emortal.velocity.liveconfig;

import dev.emortal.api.liveconfigparser.configs.LiveConfigCollection;
import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeCollection;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class LiveConfigProvider implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveConfigProvider.class);

    private final @Nullable LiveConfigCollection configCollection;

    public LiveConfigProvider() {
        ApiClient client;
        try {
            client = Config.defaultClient();
        } catch (IOException exception) {
            LOGGER.error("Failed to connect to Kubernetes! Game mode configs will not load or update!", exception);
            this.configCollection = null;
            return;
        }
        Configuration.setDefaultApiClient(client);

        LiveConfigCollection configCollection;
        try {
            configCollection = new LiveConfigCollection(client);
        } catch (IOException exception) {
            LOGGER.error("Failed to load live configs!", exception);
            configCollection = null;
        }
        this.configCollection = configCollection;
    }

    public @Nullable GameModeCollection getGameModes() {
        if (this.configCollection == null) return null;
        return this.configCollection.gameModes();
    }

    @Override
    public void close() throws IOException {
        if (this.configCollection != null) this.configCollection.close();
    }
}
