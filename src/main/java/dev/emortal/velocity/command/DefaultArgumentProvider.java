package dev.emortal.velocity.command;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import org.jetbrains.annotations.NotNull;

final class DefaultArgumentProvider implements ArgumentProvider {

    private final CommandContext<CommandSource> context;

    DefaultArgumentProvider(@NotNull CommandContext<CommandSource> context) {
        this.context = context;
    }

    @Override
    public boolean hasArgument(@NotNull String name) {
        return this.context.getArguments().containsKey(name);
    }

    @Override
    public <T> @NotNull T getArgument(@NotNull String name, @NotNull Class<T> type) {
        return this.context.getArgument(name, type);
    }
}
