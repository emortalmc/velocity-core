package dev.emortal.velocity.command;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.command.CommandExecutor;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface EmortalCommandExecutor extends CommandExecutor<CommandSource> {

    void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments);

    @Override
    default void execute(@NotNull CommandContext<CommandSource> context) {
        this.execute(context.getSource(), new DefaultArgumentProvider(context));
    }
}
