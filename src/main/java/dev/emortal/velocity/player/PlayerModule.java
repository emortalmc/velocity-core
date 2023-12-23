package dev.emortal.velocity.player;

import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.player.commands.PlaytimeCommand;
import dev.emortal.velocity.player.listener.McPlayerListener;
import dev.emortal.velocity.player.listener.PlayerJoinQuitListener;
import org.jetbrains.annotations.NotNull;

@ModuleData(name = "player")
public final class PlayerModule extends VelocityModule {

    public PlayerModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        McPlayerService service = GrpcStubCollection.getPlayerService().orElse(null);

        SessionCache sessionCache = new SessionCache();
        super.registerEventListener(sessionCache);

        if (service != null) {
            super.registerCommand(new PlaytimeCommand(service, sessionCache, super.adapters().commandManager().usernameSuggesters()));
            super.registerEventListener(new PlayerJoinQuitListener(super.adapters().audience()));
            super.registerEventListener(new McPlayerListener(service, sessionCache));
        }

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
