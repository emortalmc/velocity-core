package dev.emortal.velocity.command;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import dev.emortal.api.modules.ModuleData;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.player.PlayerServiceModule;
import dev.emortal.velocity.player.SessionCache;
import dev.emortal.velocity.player.UsernameSuggestions;
import dev.emortal.velocity.player.commands.PlaytimeCommand;
import org.jetbrains.annotations.NotNull;

@ModuleData(name = "command", required = false, softDependencies = {PlayerServiceModule.class})
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
                .plugin(this)
                .build();
        super.getProxy().getCommandManager().register(meta, new BrigadierCommand(command.build()));
    }

    @Override
    public boolean onLoad() {
        PlayerServiceModule playerServiceModule = this.getModule(PlayerServiceModule.class);
        SessionCache sessionCache = playerServiceModule != null ? playerServiceModule.getSessionCache() : null;
        McPlayerService playerService = playerServiceModule != null ? playerServiceModule.getPlayerService() : null;

        if (playerService != null) {
            this.usernameSuggestions = new UsernameSuggestions(playerServiceModule.getPlayerService());
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
