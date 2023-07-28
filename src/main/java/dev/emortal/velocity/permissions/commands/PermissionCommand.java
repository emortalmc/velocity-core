package dev.emortal.velocity.permissions.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.grpc.mcplayer.McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.velocity.general.UsernameSuggestions;
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
import dev.emortal.velocity.utils.CommandUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public final class PermissionCommand {
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
    private final UsernameSuggestions usernameSuggestions;

    private final RoleListSub roleListSub;
    private final RoleCreateSub roleCreateSub;
    private final RoleDescribeSub roleDescribeSub;
    private final RoleSetUsernameSub roleSetUsernameSub;
    private final RoleSetPrioritySub roleSetPrioritySub;
    private final RolePermissionAddSub rolePermissionAddSub;
    private final RolePermissionUnsetSub rolePermissionUnsetSub;
    private final RolePermissionCheckSub rolePermissionCheckSub;

    private final UserRoleAddSub userRoleAddSub;
    private final UserRoleRemoveSub userRoleRemoveSub;
    private final UserDescribeSub userDescribeSub;

    public PermissionCommand(@NotNull ProxyServer proxy, @NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache,
                             @NotNull UsernameSuggestions usernameSuggestions) {
        this.permissionCache = permissionCache;
        this.usernameSuggestions = usernameSuggestions;

        this.roleListSub = new RoleListSub(permissionCache);
        this.roleCreateSub = new RoleCreateSub(permissionService, permissionCache);
        this.roleDescribeSub = new RoleDescribeSub(permissionCache);
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

    private void executeBaseHelp(@NotNull CommandContext<CommandSource> context) {
        context.getSource().sendMessage(BASE_HELP_MESSAGE);
    }

    private void executeRoleHelp(@NotNull CommandContext<CommandSource> context) {
        context.getSource().sendMessage(ROLE_HELP_MESSAGE);
    }

    private void executeUserHelp(@NotNull CommandContext<CommandSource> context) {
        context.getSource().sendMessage(USER_HELP_MESSAGE);
    }

    private @NotNull CompletableFuture<Suggestions> createRoleSuggestions(@NotNull CommandContext<CommandSource> context,
                                                                          @NotNull SuggestionsBuilder builder) {
        for (String roleId : this.permissionCache.getRoleIds()) {
            if (!roleId.startsWith(builder.getRemainingLowerCase())) continue;
            builder.suggest(roleId);
        }

        return builder.buildFuture();
    }

    private @NotNull BrigadierCommand createBrigadierCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("perm")
                        .executes(CommandUtils.execute(this::executeBaseHelp))
                        .requires(source -> source.hasPermission("command.permission"))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("listroles")
                                .executes(CommandUtils.executeAsync(this.roleListSub::execute)))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("role")
                                .executes(CommandUtils.execute(this::executeRoleHelp))
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("roleId", word())
                                        .suggests(this::createRoleSuggestions)
                                        .executes(CommandUtils.executeAsync(this.roleDescribeSub::execute))
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("create")
                                                .executes(CommandUtils.executeAsync(this.roleCreateSub::execute)))
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("setusername")
                                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("usernameFormat", string())
                                                        .executes(CommandUtils.executeAsync(this.roleSetUsernameSub::execute))))
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("setpriority")
                                                .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("priority", integer(0, Integer.MAX_VALUE))
                                                        .executes(CommandUtils.executeAsync(this.roleSetPrioritySub::execute))))
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("permission")
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("set")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("permission", word())
                                                                .then(RequiredArgumentBuilder.<CommandSource, Boolean>argument("value", BoolArgumentType.bool())
                                                                        .executes(CommandUtils.executeAsync(this.rolePermissionAddSub::execute)))))
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("unset")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("permission", word())
                                                                .executes(CommandUtils.executeAsync(this.rolePermissionUnsetSub::execute))))
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("check")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("permission", word())
                                                                .executes(CommandUtils.executeAsync(this.rolePermissionCheckSub::execute)))))))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("user")
                                .executes(CommandUtils.execute(this::executeUserHelp))
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", word())
                                        .executes(CommandUtils.executeAsync(this.userDescribeSub::execute))
                                        .suggests(this.usernameSuggestions.command(FilterMethod.NONE))
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("role")
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("roleId", word())
                                                                .suggests(this::createRoleSuggestions)
                                                                .executes(CommandUtils.executeAsync(this.userRoleAddSub::execute))))
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("roleId", word())
                                                                .suggests(this::createRoleSuggestions)
                                                                .executes(CommandUtils.executeAsync(this.userRoleRemoveSub::execute)))))
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("permission")
                                                .then(LiteralArgumentBuilder.<CommandSource>literal("check")
                                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("permission", word()).executes(context -> 1))))))
        );
    }
}
