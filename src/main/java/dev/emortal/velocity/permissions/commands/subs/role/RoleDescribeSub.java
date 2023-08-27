package dev.emortal.velocity.permissions.commands.subs.role;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.permissions.PermissionCache;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public final class RoleDescribeSub implements CommandExecutor<CommandSource> {

    private final PermissionCache permissionCache;

    public RoleDescribeSub(@NotNull PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class);

        PermissionCache.CachedRole role = this.permissionCache.getRole(roleId);
        if (role == null) {
            ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, Component.text(roleId));
            return;
        }

        ChatMessages.ROLE_DESCRIPTION.send(source,
                Component.text(roleId),
                Component.text(role.priority()),
                Component.text(role.permissions().size()),
                Component.text(role.displayName()));
    }
}
