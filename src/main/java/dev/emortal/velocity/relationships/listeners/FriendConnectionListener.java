package dev.emortal.velocity.relationships.listeners;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.message.relationship.FriendConnectionMessage;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.messaging.MessagingModule;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public final class FriendConnectionListener {

    private final ProxyServer proxy;

    public FriendConnectionListener(@NotNull ProxyServer proxy, @NotNull MessagingModule messaging) {
        this.proxy = proxy;
        messaging.addListener(FriendConnectionMessage.class, message ->
                this.onFriendConnection(message.getMessageTargetIdsList(), message.getUsername(), message.getJoined()));
    }

    private void onFriendConnection(@NotNull List<String> targetIds, @NotNull String username, boolean joined) {
        for (String targetIdStr : targetIds) {
            UUID targetId = UUID.fromString(targetIdStr);

            Player target = this.proxy.getPlayer(targetId).orElse(null);
            if (target == null) continue;

            if (joined) {
                ChatMessages.FRIEND_CONNECTED.send(target, Component.text(username));
            } else {
                ChatMessages.FRIEND_DISCONNECTED.send(target, Component.text(username));
            }
        }
    }
}
