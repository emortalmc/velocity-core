package dev.emortal.velocity.permissions.listener;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.permissions.PermissionCache;
import io.grpc.StatusException;
import org.jetbrains.annotations.NotNull;

public final class PermissionCheckListener {

    private final @NotNull PermissionCache permissionCache;

    public PermissionCheckListener(@NotNull PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    @Subscribe
    public void onPermissionsSetup(@NotNull PermissionsSetupEvent event, @NotNull Continuation continuation) {
        if (!(event.getSubject() instanceof Player player)) {
            continuation.resume();
            return;
        }

        try {
            this.permissionCache.loadUser(player.getUniqueId());
        } catch (StatusException exception) {
            continuation.resumeWithException(exception);
        }

        event.setProvider(new PlayerPermissionProvider(this.permissionCache));
        continuation.resume();
    }

    private record PlayerPermissionProvider(@NotNull PermissionCache permissionCache) implements PermissionProvider {

        @Override
        public @NotNull PermissionFunction createFunction(@NotNull PermissionSubject subject) {
            return new PlayerPermissionFunction((Player) subject, this.permissionCache);
        }
    }

    private record PlayerPermissionFunction(@NotNull Player player,
                                            @NotNull PermissionCache permissionCache) implements PermissionFunction {

        @Override
        public @NotNull Tristate getPermissionValue(@NotNull String permission) {
            return this.permissionCache.getPermission(this.player.getUniqueId(), permission);
        }
    }
}
