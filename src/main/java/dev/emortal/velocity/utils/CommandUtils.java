package dev.emortal.velocity.utils;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

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
}
