package dev.emortal.velocity.party;

import dev.emortal.api.modules.annotation.Dependency;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.party.commands.PartyCommand;
import dev.emortal.velocity.party.notifier.ChatPartyUpdateNotifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(name = "party", dependencies = {@Dependency(name = "messaging")})
public final class PartyModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyModule.class);

    public PartyModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        PartyService service = GrpcStubCollection.getPartyService().orElse(null);
        if (service == null) {
            LOGGER.warn("Party service unavailable. Parties will not work.");
            return false;
        }

        MessagingModule messaging = this.getModule(MessagingModule.class);
        PartyCache cache = new PartyCache(service);
        new PartyUpdateListener(cache, super.adapters().playerProvider(), new ChatPartyUpdateNotifier(super.adapters().playerProvider()), messaging);

        super.registerCommand(new PartyCommand(service, super.playerResolver(), super.adapters().commandManager().usernameSuggesters()));

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
