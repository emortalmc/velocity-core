package dev.emortal.velocity.listener;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.message.relationship.FriendConnectionMessage;
import dev.emortal.velocity.messaging.MessagingCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public final class FriendConnectionListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String FRIEND_CONNECT_MESSAGE = "<green>Friend > <username> has connected";
    private static final String FRIEND_DISCONNECT_MESSAGE = "<green>Friend > <username> has disconnected";

    private final ProxyServer proxy;

    public FriendConnectionListener(@NotNull ProxyServer proxy, @NotNull MessagingCore messaging) {
        this.proxy = proxy;
        messaging.addListener(FriendConnectionMessage.class, message ->
                this.onFriendConnection(message.getMessageTargetIdsList(), message.getUsername(), message.getJoined()));
    }

    private void onFriendConnection(@NotNull List<String> targetIds, @NotNull String username, boolean joined) {
        for (String targetIdStr : targetIds) {
            UUID targetId = UUID.fromString(targetIdStr);

            Player target = this.proxy.getPlayer(targetId).orElse(null);
            if (target == null) continue;

            TagResolver tagResolver = Placeholder.unparsed("username", username);
            if (joined) {
                target.sendMessage(MINI_MESSAGE.deserialize(FRIEND_CONNECT_MESSAGE, tagResolver));
            } else {
                target.sendMessage(MINI_MESSAGE.deserialize(FRIEND_DISCONNECT_MESSAGE, tagResolver));
            }
        }
    }
}
