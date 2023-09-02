package dev.emortal.velocity.liveconfig;

import dev.emortal.api.modules.Module;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.modules.env.ModuleEnvironment;
import dev.emortal.velocity.Environment;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ModuleData(name = "kubernetes")
public final class KubernetesModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesModule.class);

    private @Nullable ApiClient apiClient;

    public KubernetesModule(@NotNull ModuleEnvironment environment) {
        super(environment);
    }

    public @Nullable ApiClient getApiClient() {
        return this.apiClient;
    }

    @Override
    public boolean onLoad() {
        if (!Environment.isKubernetes()) return false;

        try {
            this.apiClient = Config.defaultClient();
        } catch (IOException exception) {
            LOGGER.error("Failed to initialise Kubernetes client!", exception);
            return false;
        }

        Configuration.setDefaultApiClient(this.apiClient);
        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
