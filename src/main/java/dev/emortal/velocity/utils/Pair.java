package dev.emortal.velocity.utils;

import org.jetbrains.annotations.NotNull;

public record Pair<L, R>(@NotNull L left, @NotNull R right) {
}
