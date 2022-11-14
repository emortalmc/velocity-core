package cc.towerdefence.velocity.permissions;

import java.util.UUID;

public interface PermissionBlocker {

    boolean isBlocked(UUID playerId, String permission);

}
