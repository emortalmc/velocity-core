package dev.emortal.velocity.party;

import dev.emortal.api.modules.ModuleData;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.party.commands.PartyCommand;
import dev.emortal.velocity.player.PlayerServiceModule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(name = "party", required = false, softDependencies = {MessagingModule.class, PlayerServiceModule.class})
public final class PartyModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyModule.class);

    public PartyModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        MessagingModule messaging = this.getModule(MessagingModule.class);
        if (messaging == null) {
            LOGGER.debug("Party services cannot be loaded due to missing messaging module.");
            return false;
        }

        PartyService service = GrpcStubCollection.getPartyService().orElse(null);
        if (service == null) {
            LOGGER.warn("Party service unavailable. Parties will not work.");
            return false;
        }

        PartyCache cache = new PartyCache(super.getProxy(), service, messaging);

        PlayerServiceModule playerServiceModule = this.getModule(PlayerServiceModule.class);
        new PartyCommand(super.getProxy(), service, playerServiceModule.getUsernameSuggestions(), cache);

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
