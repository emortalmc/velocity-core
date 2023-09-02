package dev.emortal.velocity.permissions.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import org.jetbrains.annotations.NotNull;

public final class GrantAllPermissionsListener {

    @Subscribe
    public void onPermissionsSetup(@NotNull PermissionsSetupEvent event) {
        event.setProvider(subject -> PermissionFunction.ALWAYS_TRUE);
    }
}
