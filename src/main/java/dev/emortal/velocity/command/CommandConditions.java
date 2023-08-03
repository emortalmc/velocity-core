package dev.emortal.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public final class CommandConditions {

    public static @NotNull Predicate<CommandSource> playerOnly() {
        return source -> source instanceof Player;
    }

    private CommandConditions() {
    }
}
