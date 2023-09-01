package dev.emortal.velocity.agones;

import dev.emortal.api.agonessdk.AgonesUtils;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(name = "agones")
public final class AgonesModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgonesModule.class);

    public AgonesModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        AgonesGrpcStubCollection stubCollection = new AgonesGrpcStubCollection();
        if (stubCollection.getAgonesService() == null) {
            LOGGER.warn("Agones SDK unavailable. Agones features will not work.");
            return false;
        }

        super.registerEventListener(new AgonesListener(stubCollection));
        return true;
    }

    @Override
    public void onUnload() {
        AgonesUtils.shutdownHealthTask();
    }
}
