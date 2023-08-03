package dev.emortal.velocity.player;

import dev.emortal.api.modules.ModuleData;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.player.listener.McPlayerListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(name = "player-service", required = true)
public final class PlayerServiceModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerServiceModule.class);

    private final SessionCache sessionCache;

    private @Nullable McPlayerService playerService;

    public PlayerServiceModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
        this.sessionCache = new SessionCache();
    }

    public @Nullable McPlayerService getPlayerService() {
        return this.playerService;
    }

    public @NotNull SessionCache getSessionCache() {
        return this.sessionCache;
    }

    @Override
    public boolean onLoad() {
        McPlayerService service = GrpcStubCollection.getPlayerService().orElse(null);
        this.playerService = service;

        if (service == null) {
            LOGGER.warn("Player service unavailable. Global player resolution will not work.");
            return false;
        }

        super.registerEventListener(this.sessionCache);
        super.registerEventListener(new McPlayerListener(service, this.sessionCache));

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
