package dev.emortal.velocity.permissions.commands.subs.role;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
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

public final class RoleSetPrioritySub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleSetPrioritySub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String ROLE_PRIORITY_SET = "<green>Role <role_id> priority set to <priority>";

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public RoleSetPrioritySub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class);
        int priority = context.getArgument("priority", Integer.class);

        PermissionCache.CachedRole role = RoleSubUtils.getRole(this.permissionCache, context);
        if (role == null) return;

        RoleUpdate update = RoleUpdate.builder(roleId).priority(priority).build();

        UpdateRoleResult result;
        try {
            result = this.permissionService.updateRole(update);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Error while setting role priority", exception);
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Error while setting role priority"));
            return;
        }

        var roleIdPlaceholder = Placeholder.unparsed("role_id", roleId);
        var message = switch (result) {
            case UpdateRoleResult.Success(Role newRole) -> {
                this.permissionCache.setRole(newRole);
                var priorityPlaceholder = Placeholder.unparsed("priority", String.valueOf(priority));
                yield MINI_MESSAGE.deserialize(ROLE_PRIORITY_SET, roleIdPlaceholder, priorityPlaceholder);
            }
            case UpdateRoleResult.Error error -> switch (error) {
                case ROLE_NOT_FOUND -> MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, roleIdPlaceholder);
            };
        };
        source.sendMessage(message);
    }
}
