package dev.emortal.velocity.permissions.commands.subs.role;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.permissions.PermissionCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RoleListSub {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_LIST_HEADER = "<light_purple>Role List (<role_count>):";

    private static final String ROLE_LIST_LINE_HOVER = """
            ID: <role_id>
            Priority: <priority>
            Permissions: <permission_count>
            Display Name: <display_name>
                        
            Example Chat: <example_chat>""";
    private static final String ROLE_LIST_LINE = "<light_purple><hover:show_text:\"%s\"><priority>) <role_id></hover></light_purple>".formatted(ROLE_LIST_LINE_HOVER);

    private final PermissionCache permissionCache;

    public RoleListSub(@NotNull PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        List<PermissionCache.CachedRole> roles = this.permissionCache.getRoles().stream()
                .sorted(Comparator.comparingInt(PermissionCache.CachedRole::priority))
                .toList();

        List<Component> lines = new ArrayList<>();
        lines.add(MINI_MESSAGE.deserialize(ROLE_LIST_HEADER, Placeholder.unparsed("role_count", String.valueOf(roles.size()))));

        for (PermissionCache.CachedRole role : roles) {
            Component exampleChat = Component.text()
                    .append(role.formatDisplayName(source instanceof Player player ? player.getUsername() : "CONSOLE"))
                    .append(Component.text(": Test Chat", NamedTextColor.WHITE))
                    .build();

            lines.add(MINI_MESSAGE.deserialize(ROLE_LIST_LINE,
                    Placeholder.unparsed("priority", String.valueOf(role.priority())),
                    Placeholder.unparsed("role_id", role.id()),
                    Placeholder.unparsed("permission_count", String.valueOf(role.permissions().size())),
                    Placeholder.unparsed("display_name", role.displayName()),
                    Placeholder.component("example_chat", exampleChat)));
        }

        source.sendMessage(Component.join(JoinConfiguration.newlines(), lines));
    }
}
