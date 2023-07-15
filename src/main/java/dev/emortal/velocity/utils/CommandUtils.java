package dev.emortal.velocity.utils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class CommandUtils {

    public static Predicate<CommandSource> combineRequirements(Predicate<CommandSource>... requirements) {
        return source -> {
            for (Predicate<CommandSource> requirement : requirements) {
                if (!requirement.test(source)) return false;
            }
            return true;
        };
    }

    public static Predicate<CommandSource> isPlayer() {
        return source -> source instanceof Player;
    }

    public static <S> @NotNull Command<S> execute(@NotNull Consumer<CommandContext<S>> command) {
        return context -> {
            command.accept(context);
            return 1;
        };
    }

    public static <S> @NotNull Command<S> executeAsync(@NotNull Consumer<CommandContext<S>> command) {
        return context -> {
            Thread.startVirtualThread(() -> command.accept(context));
            return 1;
        };
    }
}
