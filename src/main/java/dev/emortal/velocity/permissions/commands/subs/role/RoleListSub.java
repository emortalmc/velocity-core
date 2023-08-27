package dev.emortal.velocity.permissions.commands.subs.role;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.permissions.PermissionCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RoleListSub implements CommandExecutor<CommandSource> {

    private final PermissionCache permissionCache;

    public RoleListSub(@NotNull PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        List<PermissionCache.CachedRole> roles = this.permissionCache.getRoles().stream()
                .sorted(Comparator.comparingInt(PermissionCache.CachedRole::priority))
                .toList();

        List<Component> lines = new ArrayList<>();
        lines.add(ChatMessages.ROLE_LIST_HEADER.parse(Component.text(roles.size())));

        for (PermissionCache.CachedRole role : roles) {
            Component exampleChat = Component.text()
                    .append(role.formatDisplayName(source instanceof Player player ? player.getUsername() : "CONSOLE"))
                    .append(Component.text(": Test Chat", NamedTextColor.WHITE))
                    .build();

            lines.add(ChatMessages.ROLE_LIST_LINE.parse(Component.text(role.id()),
                    Component.text(role.priority()),
                    Component.text(role.permissions().size()),
                    Component.text(role.displayName()),
                    exampleChat));
        }

        source.sendMessage(Component.join(JoinConfiguration.newlines(), lines));
    }
}
