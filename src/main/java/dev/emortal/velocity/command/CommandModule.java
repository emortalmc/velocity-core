package dev.emortal.velocity.command;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import dev.emortal.api.modules.annotation.Dependency;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.player.PlayerServiceModule;
import dev.emortal.velocity.player.SessionCache;
import dev.emortal.velocity.player.UsernameSuggestions;
import dev.emortal.velocity.player.commands.PlaytimeCommand;
import org.jetbrains.annotations.NotNull;

@ModuleData(name = "command", dependencies = {@Dependency(name = "player-service")})
public final class CommandModule extends VelocityModule {

    private @NotNull UsernameSuggestions usernameSuggestions;

    public CommandModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    public @NotNull UsernameSuggestions getUsernameSuggestions() {
        return this.usernameSuggestions;
    }

    public void registerCommand(@NotNull EmortalCommand command) {
        CommandMeta meta = super.getProxy().getCommandManager().metaBuilder(command.getName())
                .aliases(command.getAliases())
                .plugin(super.getEnvironment().plugin())
                .build();
        super.getProxy().getCommandManager().register(meta, new BrigadierCommand(command.build()));
    }

    @Override
    public boolean onLoad() {
        PlayerServiceModule playerServiceModule = this.getModule(PlayerServiceModule.class);
        SessionCache sessionCache = playerServiceModule.getSessionCache();
        McPlayerService playerService = playerServiceModule.getPlayerService();

        if (playerService != null) {
            this.usernameSuggestions = new UsernameSuggestions(playerService);
            this.registerCommand(new PlaytimeCommand(playerService, sessionCache, this.usernameSuggestions));
        } else {
            this.usernameSuggestions = new UsernameSuggestions(null);
        }

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
