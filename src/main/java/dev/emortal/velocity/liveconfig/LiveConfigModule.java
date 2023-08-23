package dev.emortal.velocity.liveconfig;

import dev.emortal.api.liveconfigparser.configs.LiveConfigCollection;
import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeCollection;
import dev.emortal.api.modules.Module;
import dev.emortal.api.modules.annotation.Dependency;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.modules.env.ModuleEnvironment;
import io.kubernetes.client.openapi.ApiClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ModuleData(name = "liveconfig", dependencies = {@Dependency(name = "kubernetes", required = false)})
public final class LiveConfigModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveConfigModule.class);

    @SuppressWarnings("NotNullFieldNotInitialized") // Where this would be null, the module will not be loaded
    private @NotNull LiveConfigCollection configCollection;

    public LiveConfigModule(@NotNull ModuleEnvironment environment) {
        super(environment);
    }

    public @Nullable GameModeCollection getGameModes() {
        return this.configCollection.gameModes();
    }

    @Override
    public boolean onLoad() {
        KubernetesModule kubernetesModule = this.getOptionalModule(KubernetesModule.class);
        if (kubernetesModule == null) {
            LOGGER.warn("Kubernetes not available. Live config module will watch files locally.");
        }
        ApiClient client = kubernetesModule != null ? kubernetesModule.getApiClient() : null;

        try {
            this.configCollection = new LiveConfigCollection(client);
            return true;
        } catch (IOException exception) {
            LOGGER.error("Failed to load live configs!", exception);
            return false;
        }
    }

    @Override
    public void onUnload() {
        try {
            this.configCollection.close();
        } catch (IOException exception) {
            LOGGER.debug("Failed to close live config collection", exception);
        }
    }
}
