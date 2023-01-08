package dev.emortal.velocity.permissions.commands.subs.role;

import dev.emortal.velocity.permissions.PermissionCache;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Optional;

public class RoleSubUtils {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";

    public static Optional<PermissionCache.Role> getRole(PermissionCache permissionCache, CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class);

        Optional<PermissionCache.Role> optionalRole = permissionCache.getRole(roleId);

        if (optionalRole.isEmpty()) source.sendMessage(MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, Placeholder.unparsed("role_id", roleId)));
        return optionalRole;
    }
}
