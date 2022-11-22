package cc.towerdefence.velocity.api.event.server;

import com.velocitypowered.api.proxy.Player;

public record SwapToTowerDefenceEvent(Player player, boolean quickJoin) {
}
