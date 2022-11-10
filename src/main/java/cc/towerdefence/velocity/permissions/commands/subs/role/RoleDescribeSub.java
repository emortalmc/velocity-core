package cc.towerdefence.velocity.permissions.commands.subs.role;

import cc.towerdefence.velocity.permissions.PermissionCache;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class RoleDescribeSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleDescribeSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String ROLE_DESCRIPTION = """
            <light_purple>----- Role Summary (<role_id>) -----
            Priority: <priority>
            Permissions: <permission_count>
            Prefix: <prefix>
            Display Name: <display_name>
            ------------<footer_addon>-----------""";

    private final PermissionCache permissionCache;

    public RoleDescribeSub(PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class);
        Optional<PermissionCache.Role> optionalRole = RoleSubUtils.getRole(this.permissionCache, context);
        if (optionalRole.isEmpty()) return 1;

        PermissionCache.Role role = optionalRole.get();
        source.sendMessage(MINI_MESSAGE.deserialize(ROLE_DESCRIPTION, Placeholder.unparsed("role_id", role.getId()),
                Placeholder.unparsed("priority", String.valueOf(role.getPriority())),
                Placeholder.unparsed("permission_count", String.valueOf(role.getPermissions().size())),
                Placeholder.component("prefix", role.getDisplayPrefix()),
                Placeholder.unparsed("display_name", role.getDisplayName()),
                Placeholder.unparsed("footer_addon", this.createFooterAddon(roleId))));

        return 1;
    }

    private String createFooterAddon(String headerText) {
        char[] footerAddon = new char[headerText.length()];
        Arrays.fill(footerAddon, '-');
        return new String(footerAddon);
    }
}
