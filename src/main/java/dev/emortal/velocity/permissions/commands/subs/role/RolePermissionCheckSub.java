package dev.emortal.velocity.permissions.commands.subs.role;

import dev.emortal.velocity.permissions.PermissionCache;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Optional;

public class RolePermissionCheckSub {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String PERMISSION_STATE = "<green>Permission <permission> state for <role_id>: <state>";

    private final PermissionCache permissionCache;

    public RolePermissionCheckSub(PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class);
        String permission = context.getArgument("permission", String.class);

        Optional<PermissionCache.CachedRole> optionalRole = RoleSubUtils.getRole(this.permissionCache, context);
        if (optionalRole.isEmpty()) return 1;

        PermissionCache.CachedRole role = optionalRole.get();
        Tristate permissionState = role.getPermissionState(permission);

        source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_STATE,
                Placeholder.unparsed("role_id", roleId),
                Placeholder.unparsed("permission", permission),
                Placeholder.unparsed("state", permissionState.toString()))
        );

        return 1;
    }
}
