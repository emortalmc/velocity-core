package dev.emortal.velocity.permissions.commands.subs.role;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import dev.emortal.api.model.permission.PermissionNode;
import dev.emortal.api.model.permission.PermissionNode.PermissionState;
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

public final class RolePermissionAddSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(RolePermissionAddSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String PERMISSION_ADDED = "<green>Permission '<permission>' set to <value> for role <role_id>";
    private static final String PERMISSION_ALREADY_EXISTS = "<red>Permission '<permission>' already set to <value> for <role_id>";

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public RolePermissionAddSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class);
        String permission = context.getArgument("permission", String.class);
        boolean value = context.getArgument("value", Boolean.class);
        Tristate tristateValue = value ? Tristate.TRUE : Tristate.FALSE;

        PermissionCache.CachedRole role = RoleSubUtils.getRole(this.permissionCache, context);
        if (role == null) return;

        var roleIdPlaceholder = Placeholder.unparsed("role_id", roleId);
        Tristate oldState = role.getPermissionState(permission);
        if (oldState == tristateValue) {
            source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_ALREADY_EXISTS,
                    roleIdPlaceholder,
                    Placeholder.unparsed("permission", permission),
                    Placeholder.unparsed("value", String.valueOf(value)))
            );
            return;
        }

        RoleUpdate.Builder updateBuilder = RoleUpdate.builder(roleId)
                .setPermission(PermissionNode.newBuilder()
                        .setState(tristateValue == Tristate.TRUE ? PermissionState.ALLOW : PermissionState.DENY)
                        .setNode(permission)
                        .build());
        if (oldState != Tristate.UNDEFINED) updateBuilder.unsetPermission(permission);
        RoleUpdate update = updateBuilder.build();

        UpdateRoleResult result;
        try {
            result = this.permissionService.updateRole(update);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Error while adding permission to role", exception);
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Error while adding permission to role"));
            return;
        }

        var message = switch (result) {
            case UpdateRoleResult.Success(Role newRole) -> {
                this.permissionCache.setRole(newRole);
                var permissionPlaceholder = Placeholder.unparsed("permission", permission);
                var valuePlaceholder = Placeholder.unparsed("value", String.valueOf(value));
                yield MINI_MESSAGE.deserialize(PERMISSION_ADDED, roleIdPlaceholder, permissionPlaceholder, valuePlaceholder);
            }
            case UpdateRoleResult.Error error -> switch (error) {
                case ROLE_NOT_FOUND -> MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, Placeholder.unparsed("role_id", roleId));
            };
        };
        source.sendMessage(message);
    }
}
