package dev.emortal.velocity.permissions;

import com.google.common.collect.Sets;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.permission.Tristate;
import dev.emortal.api.grpc.permission.PermissionProto;
import dev.emortal.api.model.permission.PermissionNode.PermissionState;
import dev.emortal.api.model.permission.Role;
import dev.emortal.api.service.permission.PermissionService;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class PermissionCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCache.class);

    private final @NotNull PermissionService permissionService;
    private final @NotNull Set<PermissionBlocker> permissionBlockers;

    private final Map<String, CachedRole> roleCache = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<UUID, User> userCache = new ConcurrentHashMap<>();

    public PermissionCache(@NotNull PermissionService permissionService, @NotNull PermissionBlocker... permissionBlockers) {
        this.permissionService = permissionService;
        this.permissionBlockers = Set.of(permissionBlockers);

        this.loadRoles();
    }

    @Blocking
    private void loadRoles() {
        List<Role> roles;
        try {
            roles = this.permissionService.getAllRoles();
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to load roles", exception);
            return;
        }

        for (Role role : roles) {
            CachedRole cachedRole = CachedRole.fromRole(role);
            this.roleCache.put(role.getId(), cachedRole);
        }
    }

    @Blocking
    public void loadUser(@NotNull UUID id) throws StatusException {
        PermissionProto.PlayerRolesResponse response;
        try {
            response = this.permissionService.getPlayerRoles(id);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to load user roles for " + id, exception);
            throw new StatusException(exception.getStatus(), exception.getTrailers());
        }

        Set<String> roleIds = Sets.newConcurrentHashSet(response.getRoleIdsList());
        User user = new User(id, roleIds);
        this.userCache.put(id, user);
    }

    public @NotNull Tristate getPermission(@NotNull UUID id, @NotNull String permission) {
        // check permission blockers
        for (PermissionBlocker permissionBlocker : this.permissionBlockers) {
            if (permissionBlocker.isBlocked(id, permission)) return Tristate.FALSE;
        }
        // continue for checks if not blocked

        User user = this.userCache.get(id);
        if (user == null) return Tristate.UNDEFINED;

        int currentPriority = 0;
        Tristate currentState = Tristate.UNDEFINED;

        for (String roleId : user.roleIds()) {
            CachedRole role = this.roleCache.get(roleId);
            if (role == null || currentPriority > role.priority()) continue;

            for (CachedRole.PermissionNode(String nodePermission, Tristate state) : role.permissions()) {
                if (!nodePermission.equals(permission)) continue;
                currentPriority = role.priority();
                currentState = state;
            }
        }

        return currentState;
    }

    public @NotNull Collection<String> getRoleIds() {
        return this.roleCache.keySet();
    }

    public @NotNull Collection<CachedRole> getRoles() {
        return this.roleCache.values();
    }

    public @Nullable CachedRole getRole(@NotNull String id) {
        return this.roleCache.get(id);
    }

    public @Nullable User getUser(@NotNull UUID id) {
        return this.userCache.get(id);
    }

    public void setRole(@NotNull Role roleResponse) {
        CachedRole role = CachedRole.fromRole(roleResponse);
        this.roleCache.put(roleResponse.getId(), role);
    }

    public boolean removeRole(@NotNull String id) {
        return this.roleCache.remove(id) != null;
    }

    public @Nullable CachedRole determinePrimaryRole(@NotNull Collection<String> roleIds) {
        int currentPriority = 0;
        CachedRole currentPrimaryRole = null;

        for (CachedRole role : this.roleCache.values()) {
            if (role.displayName() == null || !roleIds.contains(role.id())) continue;
            if (role.priority() <= currentPriority) continue;

            currentPriority = role.priority();
            currentPrimaryRole = role;
        }

        return currentPrimaryRole;
    }

    @Subscribe
    void onDisconnect(@NotNull DisconnectEvent event) {
        this.userCache.remove(event.getPlayer().getUniqueId());
    }

    public record User(@NotNull UUID id, @NotNull Set<String> roleIds) {
    }

    public record CachedRole(@NotNull String id, int priority, @Nullable String displayName,
                             @NotNull Set<PermissionNode> permissions) implements Comparable<CachedRole> {

        static @NotNull CachedRole fromRole(@NotNull Role role) {
            return new CachedRole(
                    role.getId(),
                    role.getPriority(),
                    role.hasDisplayName() ? role.getDisplayName() : null,
                    role.getPermissionsList().stream()
                            .filter(node -> node.getState() == PermissionState.ALLOW)
                            .map(CachedRole::convertPermissionNode)
                            .collect(Collectors.toCollection(Sets::newConcurrentHashSet))
            );
        }

        private static @NotNull PermissionNode convertPermissionNode(@NotNull dev.emortal.api.model.permission.PermissionNode node) {
            Tristate state = node.getState() == PermissionState.ALLOW ? Tristate.TRUE : Tristate.FALSE;
            return new PermissionNode(node.getNode(), state);
        }

        @Override
        public int compareTo(@NotNull PermissionCache.CachedRole o) {
            return Integer.compare(this.priority, o.priority);
        }

        public @NotNull Tristate getPermissionState(@NotNull String node) {
            for (PermissionNode(String permission, Tristate state) : this.permissions) {
                if (permission.equals(node)) return state;
            }
            return Tristate.UNDEFINED;
        }

        public @NotNull Component formatDisplayName(@NotNull String username) {
            if (this.displayName == null) return Component.text(username);
            return MiniMessage.miniMessage().deserialize(this.displayName, Placeholder.unparsed("username", username));
        }

        public record PermissionNode(@NotNull String permission, @NotNull Tristate state) {
        }
    }
}
