package dev.emortal.velocity.permissions;

import dev.emortal.api.modules.ModuleData;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.permissions.commands.PermissionCommand;
import dev.emortal.velocity.permissions.listener.PermissionCheckListener;
import dev.emortal.velocity.permissions.listener.PermissionUpdateListener;
import dev.emortal.velocity.player.PlayerServiceModule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(name = "permission", required = false, softDependencies = {MessagingModule.class, PlayerServiceModule.class})
public final class PermissionModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionModule.class);

    public PermissionModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        MessagingModule messaging = this.getModule(MessagingModule.class);
        if (messaging == null) {
            LOGGER.debug("Permission services cannot be loaded due to missing messaging module.");
            return false;
        }

        PermissionService service = GrpcStubCollection.getPermissionService().orElse(null);
        if (service == null) {
            LOGGER.warn("Permission service unavailable. Permissions will not work properly.");
            return false;
        }

        PermissionCache cache = new PermissionCache(service);

        super.registerEventListener(cache);
        super.registerEventListener(new PermissionCheckListener(cache));
        new PermissionUpdateListener(cache, messaging);

        PlayerServiceModule playerServiceModule = this.getModule(PlayerServiceModule.class);
        new PermissionCommand(super.getProxy(), service, cache, playerServiceModule.getUsernameSuggestions());

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
