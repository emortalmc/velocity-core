package dev.emortal.velocity.adapter.command;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import dev.emortal.velocity.CorePlugin;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.player.suggestions.UsernameSuggesterProvider;
import org.jetbrains.annotations.NotNull;

public final class VelocityEmortalCommandManager implements EmortalCommandManager {

    private final CommandManager commandManager;
    private final CorePlugin plugin;
    private final UsernameSuggesterProvider usernameSuggesterProvider;

    public VelocityEmortalCommandManager(@NotNull CommandManager commandManager, @NotNull CorePlugin plugin,
                                         @NotNull UsernameSuggesterProvider usernameSuggesterProvider) {
        this.commandManager = commandManager;
        this.plugin = plugin;
        this.usernameSuggesterProvider = usernameSuggesterProvider;
    }

    @Override
    public @NotNull UsernameSuggesterProvider usernameSuggesters() {
        return this.usernameSuggesterProvider;
    }

    @Override
    public void registerCommand(@NotNull EmortalCommand command) {
        this.commandManager.register(this.createMeta(command), new BrigadierCommand(command.build()));
    }

    private @NotNull CommandMeta createMeta(@NotNull EmortalCommand command) {
        return this.commandManager.metaBuilder(command.getName())
                .aliases(command.getAliases())
                .plugin(this.plugin)
                .build();
    }
}
