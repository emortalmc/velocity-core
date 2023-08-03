package dev.emortal.velocity.permissions.commands.subs.role;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.velocity.permissions.PermissionCache;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class RoleDescribeSub implements CommandExecutor<CommandSource> {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String ROLE_DESCRIPTION = """
            <light_purple>----- Role Summary (<role_id>) -----
            Priority: <priority>
            Permissions: <permission_count>
            Display Name: <display_name>
            ------------<footer_addon>-----------""";

    private final PermissionCache permissionCache;

    public RoleDescribeSub(@NotNull PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class);

        var roleIdPlaceholder = Placeholder.unparsed("role_id", roleId);
        PermissionCache.CachedRole role = RoleSubUtils.getRole(this.permissionCache, context);
        if (role == null) {
            source.sendMessage(MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, roleIdPlaceholder));
            return;
        }

        source.sendMessage(MINI_MESSAGE.deserialize(ROLE_DESCRIPTION, roleIdPlaceholder,
                Placeholder.unparsed("priority", String.valueOf(role.priority())),
                Placeholder.unparsed("permission_count", String.valueOf(role.permissions().size())),
                Placeholder.unparsed("display_name", role.displayName()),
                Placeholder.unparsed("footer_addon", this.createFooterAddon(roleId))));
    }

    private @NotNull String createFooterAddon(@NotNull String headerText) {
        char[] footerAddon = new char[headerText.length()];
        Arrays.fill(footerAddon, '-');
        return new String(footerAddon);
    }
}
