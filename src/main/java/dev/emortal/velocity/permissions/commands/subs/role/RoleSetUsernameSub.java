package dev.emortal.velocity.permissions.commands.subs.role;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.permission.Role;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.api.service.permission.RoleUpdate;
import dev.emortal.api.service.permission.UpdateRoleResult;
import dev.emortal.velocity.permissions.PermissionCache;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleSetUsernameSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleSetUsernameSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String ROLE_USERNAME_SET = "<green>Role <role_id> username set to \"<username_format>\" (e.g <formatted_username><green>)";

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public RoleSetUsernameSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    public void execute(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class).toLowerCase();
        String usernameFormat = context.getArgument("usernameFormat", String.class);

        PermissionCache.CachedRole role = RoleSubUtils.getRole(this.permissionCache, context);
        if (role == null) return;

        RoleUpdate update = RoleUpdate.builder(roleId).displayName(usernameFormat).build();

        UpdateRoleResult result;
        try {
            result = this.permissionService.updateRole(update);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to set role username", exception);
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to set role username"));
            return;
        }

        var message = switch (result) {
            case UpdateRoleResult.Success(Role newRole) -> {
                this.permissionCache.setRole(newRole);

                Component formattedUsername = role.formatDisplayName(source instanceof Player player ? player.getUsername() : "CONSOLE");
                var roleIdPlaceholder = Placeholder.unparsed("role_id", roleId);
                var usernameFormatPlaceholder = Placeholder.unparsed("username_format", usernameFormat);
                var usernameComponent = MINI_MESSAGE.deserialize(usernameFormat, Placeholder.component("username", formattedUsername));
                var formattedUsernamePlaceholder = Placeholder.component("formatted_username", usernameComponent);

                yield MINI_MESSAGE.deserialize(ROLE_USERNAME_SET, roleIdPlaceholder, usernameFormatPlaceholder, formattedUsernamePlaceholder);
            }
            case UpdateRoleResult.Error error -> switch (error) {
                case ROLE_NOT_FOUND -> MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, Placeholder.unparsed("role_id", roleId));
            };
        };
        source.sendMessage(message);
    }
}
