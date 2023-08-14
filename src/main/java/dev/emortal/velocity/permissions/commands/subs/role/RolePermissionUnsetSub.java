package dev.emortal.velocity.permissions.commands.subs.role;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.model.permission.Role;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.api.service.permission.RoleUpdate;
import dev.emortal.api.service.permission.UpdateRoleResult;
import dev.emortal.velocity.permissions.PermissionCache;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RolePermissionUnsetSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RolePermissionUnsetSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String PERMISSION_UNSET = "<green>Permission <permission> unset from role <role_id>";
    private static final String PERMISSION_NOT_FOUND = "<red>Permission <permission> not found for role <role_id>";

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public RolePermissionUnsetSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class);
        String permission = context.getArgument("permission", String.class);

        var roleIdPlaceholder = Placeholder.unparsed("role_id", roleId);
        PermissionCache.CachedRole role = RoleSubUtils.getRole(this.permissionCache, context);
        if (role == null) return;

        if (role.getPermissionState(permission) == Tristate.UNDEFINED) {
            source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_NOT_FOUND, roleIdPlaceholder, Placeholder.unparsed("permission", permission)));
            return;
        }

        RoleUpdate update = RoleUpdate.builder(roleId).unsetPermission(permission).build();

        UpdateRoleResult result;
        try {
            result = this.permissionService.updateRole(update);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Error while unsetting permission from role", exception);
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Error while unsetting permission from role"));
            return;
        }

        var message = switch (result) {
            case UpdateRoleResult.Success(Role newRole) -> {
                this.permissionCache.setRole(newRole);
                yield MINI_MESSAGE.deserialize(PERMISSION_UNSET, roleIdPlaceholder, Placeholder.unparsed("permission", permission));
            }
            case UpdateRoleResult.Error error -> switch (error) {
                case ROLE_NOT_FOUND -> MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, roleIdPlaceholder);
            };
        };
        source.sendMessage(message);
    }
}
