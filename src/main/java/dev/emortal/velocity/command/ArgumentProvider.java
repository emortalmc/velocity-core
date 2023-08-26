package dev.emortal.velocity.command;

import org.jetbrains.annotations.NotNull;

public interface ArgumentProvider {

    boolean hasArgument(@NotNull String name);

    <T> @NotNull T getArgument(@NotNull String name, @NotNull Class<T> type);
}
