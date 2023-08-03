package dev.emortal.velocity.player;

import dev.emortal.api.modules.ModuleData;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.player.commands.PlaytimeCommand;
import dev.emortal.velocity.player.listener.McPlayerListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(name = "player-service", required = true)
public final class PlayerServiceModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerServiceModule.class);

    private @Nullable McPlayerService playerService;
    private @NotNull UsernameSuggestions usernameSuggestions;

    public PlayerServiceModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
        this.usernameSuggestions = new UsernameSuggestions(null);
    }

    public @Nullable McPlayerService getPlayerService() {
        return this.playerService;
    }

    public @NotNull UsernameSuggestions getUsernameSuggestions() {
        return this.usernameSuggestions;
    }

    @Override
    public boolean onLoad() {
        SessionCache sessionCache = new SessionCache();
        super.registerEventListener(sessionCache);

        McPlayerService service = GrpcStubCollection.getPlayerService().orElse(null);
        this.playerService = service;
        this.usernameSuggestions = new UsernameSuggestions(service);

        if (service == null) {
            LOGGER.warn("Player service unavailable. Global player resolution will not work.");
            return true;
        }

        super.registerEventListener(new McPlayerListener(service, sessionCache));
        new PlaytimeCommand(super.getProxy(), service, sessionCache, this.usernameSuggestions);

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
