package dev.emortal.velocity.misc.listener;

import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.service.playertracker.PlayerTrackerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.adapter.event.EmortalEventManager;
import dev.emortal.velocity.adapter.scheduler.EmortalScheduler;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import org.jetbrains.annotations.NotNull;

@ModuleData(name = "core-listeners")
public final class CoreListenersModule extends VelocityModule {

    public CoreListenersModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        EmortalEventManager eventManager = super.adapters().eventManager();
        EmortalScheduler scheduler = super.adapters().scheduler();

        // transfer cookie verification
        eventManager.register(new LoginCookieVerification());

        // tablist
        PlayerTrackerService playerTracker = GrpcStubCollection.getPlayerTrackerService().orElse(null);
        eventManager.register(new TabList(scheduler, super.adapters().playerProvider(), playerTracker));

        // resource pack
        eventManager.register(new ResourcePackSender(super.adapters().resourcePackProvider(), scheduler));

        // fuck lunar
        eventManager.register(new LunarKicker());

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
