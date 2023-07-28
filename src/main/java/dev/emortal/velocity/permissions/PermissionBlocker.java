package dev.emortal.velocity.permissions;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface PermissionBlocker {

    boolean isBlocked(@NotNull UUID playerId, @NotNull String permission);
}
