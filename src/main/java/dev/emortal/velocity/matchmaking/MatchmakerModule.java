package dev.emortal.velocity.matchmaking;

import dev.emortal.api.modules.annotation.Dependency;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.service.matchmaker.MatchmakerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.matchmaking.commands.LobbyCommand;
import dev.emortal.velocity.matchmaking.listener.LobbySelectorListener;
import dev.emortal.velocity.matchmaking.listener.ServerChangeNotificationListener;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(name = "matchmaker", dependencies = {@Dependency(name = "messaging")})
public final class MatchmakerModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchmakerModule.class);

    public MatchmakerModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        MatchmakerService service = GrpcStubCollection.getMatchmakerService().orElse(null);
        if (service == null) {
            LOGGER.warn("Matchmaker service unavailable. Matchmaking services will not work.");
            return false;
        }

        super.registerCommand(new LobbyCommand(service));

        MessagingModule messaging = this.getModule(MessagingModule.class);
        new ServerChangeNotificationListener(super.adapters().playerProvider(), super.adapters().serverProvider(), messaging);
        super.registerEventListener(new LobbySelectorListener(super.adapters().serverProvider(), service, messaging));

        return true;
    }

    @Override
    public void onUnload() {
    }
}
