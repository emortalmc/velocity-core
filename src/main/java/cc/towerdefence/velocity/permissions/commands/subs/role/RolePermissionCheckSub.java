package cc.towerdefence.velocity.permissions.commands.subs.role;

import cc.towerdefence.api.service.PermissionProto;
import cc.towerdefence.api.service.PermissionServiceGrpc;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import cc.towerdefence.velocity.permissions.PermissionCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import io.grpc.Status;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

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

        Optional<PermissionCache.Role> optionalRole = RoleSubUtils.getRole(this.permissionCache, context);
        if (optionalRole.isEmpty()) return 1;

        PermissionCache.Role role = optionalRole.get();
        Tristate permissionState = role.getPermissionState(permission);

        source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_STATE,
                Placeholder.unparsed("role_id", roleId),
                Placeholder.unparsed("permission", permission),
                Placeholder.unparsed("state", permissionState.toString()))
        );

        return 1;
    }
}
