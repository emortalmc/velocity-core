package dev.emortal.velocity.permissions.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.command.element.ArgumentElement;
import dev.emortal.api.grpc.mcplayer.McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.UsernameSuggestions;
import dev.emortal.velocity.permissions.PermissionCache;
import dev.emortal.velocity.permissions.commands.subs.role.RoleCreateSub;
import dev.emortal.velocity.permissions.commands.subs.role.RoleDescribeSub;
import dev.emortal.velocity.permissions.commands.subs.role.RoleListSub;
import dev.emortal.velocity.permissions.commands.subs.role.RolePermissionAddSub;
import dev.emortal.velocity.permissions.commands.subs.role.RolePermissionCheckSub;
import dev.emortal.velocity.permissions.commands.subs.role.RolePermissionUnsetSub;
import dev.emortal.velocity.permissions.commands.subs.role.RoleSetPrioritySub;
import dev.emortal.velocity.permissions.commands.subs.role.RoleSetUsernameSub;
import dev.emortal.velocity.permissions.commands.subs.user.UserDescribeSub;
import dev.emortal.velocity.permissions.commands.subs.user.UserRoleAddSub;
import dev.emortal.velocity.permissions.commands.subs.user.UserRoleRemoveSub;
import org.jetbrains.annotations.NotNull;

public final class PermissionCommand extends EmortalCommand {

    public PermissionCommand(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache,
                             @NotNull UsernameSuggestions usernameSuggestions) {
        super("perm");

        super.setCondition(source -> source.hasPermission("command.permission"));
        super.setDefaultExecutor(context -> ChatMessages.PERMISSION_HELP.send(context.getSource()));

        // /perm listroles
        super.addSyntax(new RoleListSub(permissionCache), literal("listroles"));

        var roleIdArgument = argument("roleId", StringArgumentType.word(), this.createRoleSuggestions(permissionCache));

        // /perm role
        super.addSubCommand(new RoleParentSub(permissionService, permissionCache, roleIdArgument));

        // /perm user
        super.addSubCommand(new UserParentSub(permissionService, permissionCache, usernameSuggestions, roleIdArgument));
    }

    private @NotNull SuggestionProvider<CommandSource> createRoleSuggestions(@NotNull PermissionCache permissionCache) {
        return (context, builder) -> {
            for (String roleId : permissionCache.getRoleIds()) {
                if (!roleId.startsWith(builder.getRemainingLowerCase())) continue;
                builder.suggest(roleId);
            }

            return builder.buildFuture();
        };
    }

    private static final class RoleParentSub extends EmortalCommand {

        RoleParentSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache,
                      @NotNull ArgumentElement<CommandSource, String> roleIdArgument) {
            super("role");
            super.setDefaultExecutor(context -> ChatMessages.ROLE_HELP.send(context.getSource()));

            super.addSyntax(new RoleDescribeSub(permissionCache), roleIdArgument);
            super.addSyntax(new RoleCreateSub(permissionService, permissionCache), roleIdArgument, literal("create"));

            var usernameArgument = argument("usernameFormat", StringArgumentType.string(), null);
            super.addSyntax(new RoleSetUsernameSub(permissionService, permissionCache), roleIdArgument, literal("setusername"), usernameArgument);

            var priorityArgument = argument("priority", IntegerArgumentType.integer(0, Integer.MAX_VALUE), null);
            super.addSyntax(new RoleSetPrioritySub(permissionService, permissionCache), roleIdArgument, literal("setpriority"), priorityArgument);

            var permission = literal("permission");
            var permissionArgument = argument("permission", StringArgumentType.word(), null);
            var permissionSetValue = argument("value", BoolArgumentType.bool(), null);
            super.addSyntax(new RolePermissionAddSub(permissionService, permissionCache), roleIdArgument, permission, literal("set"), permissionArgument, permissionSetValue);
            super.addSyntax(new RolePermissionUnsetSub(permissionService, permissionCache), roleIdArgument, permission, literal("unset"), permissionArgument);
            super.addSyntax(new RolePermissionCheckSub(permissionCache), roleIdArgument, permission, literal("check"), permissionArgument);
        }
    }

    private static final class UserParentSub extends EmortalCommand {

        UserParentSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache,
                      @NotNull UsernameSuggestions usernameSuggestions, @NotNull ArgumentElement<CommandSource, String> roleIdArgument) {
            super("user");
            super.setDefaultExecutor(context -> ChatMessages.USER_HELP.send(context.getSource()));

            var usernameArgument = argument("username", StringArgumentType.word(), usernameSuggestions.command(FilterMethod.NONE));
            super.addSyntax(new UserDescribeSub(permissionService, permissionCache), usernameArgument);

            super.addSyntax(new UserRoleAddSub(permissionService, permissionCache), usernameArgument, literal("role"), literal("add"), roleIdArgument);
            super.addSyntax(new UserRoleRemoveSub(permissionService, permissionCache), usernameArgument, literal("role"), literal("remove"), roleIdArgument);

            var permissionArgument = argument("permission", StringArgumentType.word(), null);
            super.addSyntax(context -> {}, literal("permission"), literal("check"), permissionArgument);
        }
    }
}
