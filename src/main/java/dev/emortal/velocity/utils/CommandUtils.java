package dev.emortal.velocity.utils;

import org.jetbrains.annotations.NotNull;

public final class CommandUtils {

    private CommandUtils() {
    }

    public static @NotNull String getCommandName(@NotNull String command) {
        int index = command.indexOf(' ');
        if (index == -1) {
            return command;
        }

        return command.substring(0, index);
    }
}
