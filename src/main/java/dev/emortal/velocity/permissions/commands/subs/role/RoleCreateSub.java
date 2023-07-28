package dev.emortal.velocity.permissions.commands.subs.role;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.model.permission.Role;
import dev.emortal.api.service.permission.CreateRoleResult;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.velocity.permissions.PermissionCache;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoleCreateSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleCreateSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_ALREADY_EXISTS = "<red>Role <role_id> already exists";
    private static final String ROLE_CREATED = "<green>Role <role_id> created";

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public RoleCreateSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class).toLowerCase();

        CreateRoleResult result;
        try {
            result = this.permissionService.createRole(roleId, 0, "<username>");
        } catch (StatusRuntimeException exception) {
            LOGGER.error("An error occurred while creating role", exception);
            source.sendMessage(Component.text("An error occurred while creating role", NamedTextColor.RED));
            return;
        }

        var message = switch (result) {
            case CreateRoleResult.Success(Role role) -> {
                this.permissionCache.setRole(role);
                yield MINI_MESSAGE.deserialize(ROLE_CREATED, Placeholder.unparsed("role_id", role.getId()));
            }
            case CreateRoleResult.Error error -> switch (error) {
                case ROLE_ALREADY_EXISTS -> MINI_MESSAGE.deserialize(ROLE_ALREADY_EXISTS, Placeholder.unparsed("role_id", roleId));
            };
        };
        source.sendMessage(message);
    }
}
