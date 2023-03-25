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

public class PermissionCheckListener {
    private final PermissionCache permissionCache;

    public PermissionCheckListener(PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    @Subscribe
    public void onPermissionsSetup(PermissionsSetupEvent event, Continuation continuation) {
        if (!(event.getSubject() instanceof Player player)) {
            continuation.resume();
            return;
        }

        this.permissionCache.loadUser(player.getUniqueId(), continuation::resume, continuation::resumeWithException);
        event.setProvider(new PlayerPermissionProvider(this.permissionCache));
    }

    private static class PlayerPermissionProvider implements PermissionProvider {
        private final PermissionCache permissionCache;

        private PlayerPermissionProvider(PermissionCache permissionCache) {
            this.permissionCache = permissionCache;
        }

        @Override
        public PermissionFunction createFunction(PermissionSubject subject) {
            return new PlayerPermissionFunction((Player) subject, this.permissionCache);
        }
    }

    private static class PlayerPermissionFunction implements PermissionFunction {
        private final Player player;
        private final PermissionCache permissionCache;

        public PlayerPermissionFunction(Player player, PermissionCache permissionCache) {
            this.player = player;
            this.permissionCache = permissionCache;
        }

        @Override
        public Tristate getPermissionValue(String permission) {
            return this.permissionCache.getPermission(this.player.getUniqueId(), permission);
        }
    }
}
