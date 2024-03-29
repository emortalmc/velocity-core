package dev.emortal.velocity.permissions;

import dev.emortal.api.modules.annotation.Dependency;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.Environment;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.permissions.commands.PermissionCommand;
import dev.emortal.velocity.permissions.listener.GrantAllPermissionsListener;
import dev.emortal.velocity.permissions.listener.PermissionCheckListener;
import dev.emortal.velocity.permissions.listener.PermissionUpdateListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(name = "permission", dependencies = {@Dependency(name = "messaging")})
public final class PermissionModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionModule.class);

    // Grants all permissions if the permission service is not available
    private static final boolean GRANT_ALL_PERMISSIONS = Boolean.parseBoolean(System.getenv("GRANT_ALL_PERMISSIONS"));

    public PermissionModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        if (!Environment.isProduction()) {
            if (GRANT_ALL_PERMISSIONS) {
                LOGGER.warn("Permission service unavailable. Granting all permissions.");
                super.registerEventListener(new GrantAllPermissionsListener());
            } else {
                LOGGER.warn("Permission service unavailable. Denying all permissions.");
            }

            return true;
        }

        PermissionService service = GrpcStubCollection.getPermissionService().orElse(null);
        if (service == null) {
            LOGGER.warn("Permission service unavailable. Permissions will not work properly.");
            return false;
        }

        MessagingModule messaging = this.getModule(MessagingModule.class);
        PermissionCache cache = new PermissionCache(service);

        super.registerEventListener(cache);
        super.registerEventListener(new PermissionCheckListener(cache));
        new PermissionUpdateListener(cache, messaging);

        super.registerCommand(new PermissionCommand(service, cache, super.playerResolver(), super.adapters().commandManager().usernameSuggesters()));
        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
