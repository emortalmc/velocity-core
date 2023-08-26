package dev.emortal.velocity.player.resolver;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record CachedMcPlayer(@NotNull UUID uuid, @NotNull String username, boolean online) {
}
