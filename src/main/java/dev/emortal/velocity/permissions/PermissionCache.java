package dev.emortal.velocity.permissions;

import com.google.common.collect.Sets;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.permission.Tristate;
import dev.emortal.api.grpc.permission.PermissionProto;
import dev.emortal.api.model.permission.PermissionNode.PermissionState;
import dev.emortal.api.model.permission.Role;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.api.utils.GrpcStubCollection;
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

public class PermissionCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCache.class);

    private final Map<String, CachedRole> roleCache = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<UUID, User> userCache = new ConcurrentHashMap<>();

    private final PermissionService permissionService;
    private final Set<PermissionBlocker> permissionBlockers;

    public PermissionCache(PermissionBlocker... permissionBlockers) {
        this.permissionService = GrpcStubCollection.getPermissionService().orElse(null);
        this.permissionBlockers = Set.of(permissionBlockers);

        this.loadRoles();
    }

    @Blocking
    private void loadRoles() {
        if (this.permissionService == null) {
            LOGGER.warn("Permission service is not available! Not loading roles");
            return;
        }

        List<Role> roles;
        try {
            roles = this.permissionService.getAllRoles();
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to load roles", exception);
            return;
        }

        for (Role role : roles) {
            var cachedRole = CachedRole.fromRole(role);
            this.roleCache.put(role.getId(), cachedRole);
        }
    }

    @Blocking
    public void loadUser(UUID id) throws StatusException {
        PermissionProto.PlayerRolesResponse response;
        try {
            response = this.permissionService.getPlayerRoles(id);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to load user roles for " + id, exception);
            throw new StatusException(exception.getStatus(), exception.getTrailers());
        }

        Set<String> roleIds = Sets.newConcurrentHashSet(response.getRoleIdsList());
        User user = new User(id, roleIds, this.determineActiveName(roleIds));
        this.userCache.put(id, user);
    }

    public Tristate getPermission(UUID id, String permission) {
        // check permission blockers
        for (PermissionBlocker permissionBlocker : this.permissionBlockers) {
            if (permissionBlocker.isBlocked(id, permission)) return Tristate.FALSE;
        }
        // continue for checks if not blocked

        User user = this.userCache.get(id);
        if (user == null) {
            return Tristate.UNDEFINED;
        }

        int currentPriority = 0;
        Tristate currentState = Tristate.UNDEFINED;

        for (String roleId : user.getRoleIds()) {
            CachedRole role = this.roleCache.get(roleId);
            if (role == null || currentPriority > role.getPriority()) continue;

            for (CachedRole.PermissionNode node : role.getPermissions()) {
                if (node.permission().equals(permission)) {
                    currentPriority = role.getPriority();
                    currentState = node.state();
                }
            }
        }

        return currentState;
    }

    public Map<String, CachedRole> getRoleCache() {
        return roleCache;
    }

    public Map<UUID, User> getUserCache() {
        return userCache;
    }

    public @Nullable CachedRole getRole(String id) {
        return this.roleCache.get(id);
    }

    public @Nullable User getUser(UUID id) {
        return this.userCache.get(id);
    }

    public void setRole(@NotNull Role roleResponse) {
        var role = CachedRole.fromRole(roleResponse);
        this.roleCache.put(roleResponse.getId(), role);
    }

    public boolean removeRole(String id) {
        return this.roleCache.remove(id) != null;
    }

    public String determineActiveName(Collection<String> roleIds) {
        int currentPriority = 0;
        String currentActiveName = null;

        for (CachedRole role : this.roleCache.values()) {
            if (role.getDisplayName() != null && roleIds.contains(role.id())) {
                if (role.getPriority() > currentPriority) {
                    currentPriority = role.getPriority();
                    currentActiveName = role.getDisplayName();
                }
            }
        }
        return currentActiveName;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        this.userCache.remove(event.getPlayer().getUniqueId());
    }

    public static final class User {
        private final UUID id;
        private final Set<String> roleIds;

        private String displayName;

        public User(UUID id, Set<String> roleIds, String displayName) {
            this.id = id;
            this.roleIds = roleIds;
            this.displayName = displayName;
        }

        public UUID getId() {
            return this.id;
        }

        public Set<String> getRoleIds() {
            return this.roleIds;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    public record CachedRole(@NotNull String id, int priority, @NotNull String displayName,
                             @NotNull Set<PermissionNode> permissions) implements Comparable<CachedRole> {

        static @NotNull CachedRole fromRole(@NotNull Role role) {
            return new CachedRole(
                    role.getId(),
                    role.getPriority(),
                    role.getDisplayName(),
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

        public String getId() {
            return this.id;
        }

        public Set<PermissionNode> getPermissions() {
            return this.permissions;
        }

        public Tristate getPermissionState(String node) {
            for (PermissionNode permissionNode : this.permissions) {
                if (permissionNode.permission().equals(node)) {
                    return permissionNode.state();
                }
            }
            return Tristate.UNDEFINED;
        }

        public int getPriority() {
            return this.priority;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public @NotNull Component formatDisplayName(@NotNull String username) {
            return MiniMessage.miniMessage().deserialize(this.displayName, Placeholder.unparsed("username", username));
        }

        public record PermissionNode(String permission, Tristate state) {
        }
    }
}
