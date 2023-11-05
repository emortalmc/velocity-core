package dev.emortal.velocity.misc.command;

import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.service.playertracker.PlayerTrackerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.misc.DiscordCommand;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import org.jetbrains.annotations.NotNull;

@ModuleData(name = "core-commands")
public class CoreCommandsModule extends VelocityModule {

    public CoreCommandsModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        PlayerTrackerService playerTracker = GrpcStubCollection.getPlayerTrackerService().orElse(null);
        super.registerCommand(new ListCommand(playerTracker));
        super.registerCommand(new DiscordCommand());

        return true;
    }

    @Override
    public void onUnload() {

    }
}
