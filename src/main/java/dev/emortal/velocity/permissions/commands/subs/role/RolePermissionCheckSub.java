package dev.emortal.velocity.permissions.commands.subs.role;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.velocity.permissions.PermissionCache;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

public final class RolePermissionCheckSub implements CommandExecutor<CommandSource> {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String PERMISSION_STATE = "<green>Permission <permission> state for <role_id>: <state>";

    private final PermissionCache permissionCache;

    public RolePermissionCheckSub(@NotNull PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class);
        String permission = context.getArgument("permission", String.class);

        PermissionCache.CachedRole role = RoleSubUtils.getRole(this.permissionCache, context);
        if (role == null) return;

        Tristate permissionState = role.getPermissionState(permission);
        source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_STATE,
                Placeholder.unparsed("role_id", roleId),
                Placeholder.unparsed("permission", permission),
                Placeholder.unparsed("state", permissionState.toString()))
        );
    }
}
