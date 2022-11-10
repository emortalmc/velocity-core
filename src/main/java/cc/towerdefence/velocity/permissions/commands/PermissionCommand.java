package cc.towerdefence.velocity.permissions.commands;

import cc.towerdefence.api.service.PermissionServiceGrpc;
import cc.towerdefence.velocity.permissions.PermissionCache;
import cc.towerdefence.velocity.permissions.commands.subs.role.RoleCreateSub;
import cc.towerdefence.velocity.permissions.commands.subs.role.RoleDescribeSub;
import cc.towerdefence.velocity.permissions.commands.subs.role.RoleListSub;
import cc.towerdefence.velocity.permissions.commands.subs.role.RolePermissionAddSub;
import cc.towerdefence.velocity.permissions.commands.subs.role.RolePermissionCheckSub;
import cc.towerdefence.velocity.permissions.commands.subs.role.RolePermissionUnsetSub;
import cc.towerdefence.velocity.permissions.commands.subs.role.RoleSetPrefixSub;
import cc.towerdefence.velocity.permissions.commands.subs.role.RoleSetPrioritySub;
import cc.towerdefence.velocity.permissions.commands.subs.role.RoleSetUsernameSub;
import cc.towerdefence.velocity.permissions.commands.subs.user.UserDescribeSub;
import cc.towerdefence.velocity.permissions.commands.subs.user.UserRoleAddSub;
import cc.towerdefence.velocity.permissions.commands.subs.user.UserRoleRemoveSub;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class PermissionCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component BASE_HELP_MESSAGE = MINI_MESSAGE.deserialize(
            """
                    <light_purple>-- Permission Help --
                    <click:suggest_command:'/perm role'>/perm role</click>
                    <click:suggest_command:'/perm user'>/perm user</click>
                    <click:suggest_command:'/perm listroles'>/perm listroles</click>
                    ------------------"""
    );

    private static final Component ROLE_HELP_MESSAGE = MINI_MESSAGE.deserialize("""
            <light_purple>--------- Role Permission Help ---------
            <click:suggest_command:'/perm role '>/perm role <name> create</click>
            <click:suggest_command:'/perm role '>/perm role <name> setprefix <prefix></click>
            <click:suggest_command:'/perm role '>/perm role <name> setusername <state></click>
            <click:suggest_command:'/perm role '>/perm role <name> setpriority <state></click>
            <click:suggest_command:'/perm role '>/perm role <name> permission add <perm></click>
            <click:suggest_command:'/perm role '>/perm role <name> permission unset <perm></click>
            <click:suggest_command:'/perm role '>/perm role <name> permission check <perm></click>
            ------------------------------------""");

    private static final Component USER_HELP_MESSAGE = MINI_MESSAGE.deserialize("""
            <light_purple>--------- User Permission Help ---------
            <click:suggest_command:'/perm user '>/perm user <name> role add <group></click>
            <click:suggest_command:'/perm user '>/perm user <name> role remove <group></click>
            <click:suggest_command:'/perm user '>/perm user <name> permission check <perm></click>
            ------------------------------------""");

    private final PermissionCache permissionCache;

    private final RoleListSub roleListSub;
    private final RoleCreateSub roleCreateSub;
    private final RoleDescribeSub roleDescribeSub;
    private final RoleSetPrefixSub roleSetPrefixSub;
    private final RoleSetUsernameSub roleSetUsernameSub;
    private final RoleSetPrioritySub roleSetPrioritySub;
    private final RolePermissionAddSub rolePermissionAddSub;
    private final RolePermissionUnsetSub rolePermissionUnsetSub;
    private final RolePermissionCheckSub rolePermissionCheckSub;

    private final UserRoleAddSub userRoleAddSub;
    private final UserRoleRemoveSub userRoleRemoveSub;
    private final UserDescribeSub userDescribeSub;

    public PermissionCommand(ProxyServer proxy, PermissionServiceGrpc.PermissionServiceFutureStub permissionService, PermissionCache permissionCache) {
        this.permissionCache = permissionCache;

        this.roleListSub = new RoleListSub(permissionCache);
        this.roleCreateSub = new RoleCreateSub(permissionService, permissionCache);
        this.roleDescribeSub = new RoleDescribeSub(permissionCache);
        this.roleSetPrefixSub = new RoleSetPrefixSub(permissionService, permissionCache);
        this.roleSetUsernameSub = new RoleSetUsernameSub(permissionService, permissionCache);
        this.roleSetPrioritySub = new RoleSetPrioritySub(permissionService, permissionCache);
        this.rolePermissionAddSub = new RolePermissionAddSub(permissionService, permissionCache);
        this.rolePermissionUnsetSub = new RolePermissionUnsetSub(permissionService, permissionCache);
        this.rolePermissionCheckSub = new RolePermissionCheckSub(permissionCache);

        this.userRoleAddSub = new UserRoleAddSub(permissionService, permissionCache);
        this.userRoleRemoveSub = new UserRoleRemoveSub(permissionService, permissionCache);
        this.userDescribeSub = new UserDescribeSub(permissionService, permissionCache);

        proxy.getCommandManager().register(this.createBrigadierCommand());
    }

    private int executeBaseHelp(CommandContext<CommandSource> context) {
        context.getSource().sendMessage(BASE_HELP_MESSAGE);
        return 1;
    }

    private int executeRoleHelp(CommandContext<CommandSource> context) {
        context.getSource().sendMessage(ROLE_HELP_MESSAGE);
        return 1;
    }

    private int executeUserHelp(CommandContext<CommandSource> context) {
        context.getSource().sendMessage(USER_HELP_MESSAGE);
        return 1;
    }

    private CompletableFuture<Suggestions> createRoleSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        this.permissionCache.getRoleCache().keySet().stream()
                .filter(roleId -> roleId.startsWith(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);

        return builder.buildFuture();
    }

    private BrigadierCommand createBrigadierCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("perm")
                        .executes(this::executeBaseHelp)
                        .then(LiteralArgumentBuilder.<CommandSource>literal("listroles")
                                .executes(this.roleListSub::execute)
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("role")
                                .executes(this::executeRoleHelp)
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("roleId", word())
                                        .suggests(this::createRoleSuggestions)
                                        .executes(this.roleDescribeSub::execute)
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("create")
                                                .executes(this.roleCreateSub::execute)
                                        )
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("setprefix")
                                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("prefix", string())
                                                        .executes(this.roleSetPrefixSub::execute)
                                                )
                                        )
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("setusername")
                                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("usernameFormat", string())
                                                        .executes(this.roleSetUsernameSub::execute)
                                                )
                                        )
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("setpriority")
                                                .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("priority", integer(0, Integer.MAX_VALUE))
                                                        .executes(this.roleSetPrioritySub::execute)
                                                )
                                        )
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("permission")
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("permission", word())
                                                                .executes(this.rolePermissionAddSub::execute)
                                                        )
                                                )
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("unset")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("permission", word())
                                                                .executes(this.rolePermissionUnsetSub::execute)
                                                        )
                                                )
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("check")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("permission", word())
                                                                .executes(this.rolePermissionCheckSub::execute)
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("user")
                                .executes(this::executeUserHelp)
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", word())
                                        .executes(this.userDescribeSub::execute) // describe
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("role")
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("roleId", word())
                                                                .suggests(this::createRoleSuggestions)
                                                                .executes(this.userRoleAddSub::execute)
                                                        )
                                                )
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("roleId", word())
                                                                .suggests(this::createRoleSuggestions)
                                                                .executes(this.userRoleRemoveSub::execute)
                                                        )
                                                )
                                        )
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("permission")
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("check")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("permission", word())
                                                                .executes(context -> 1)
                                                        )
                                                )
                                        )
                                )
                        )
        );
    }
}
