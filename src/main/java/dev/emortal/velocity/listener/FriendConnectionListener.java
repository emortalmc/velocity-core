package dev.emortal.velocity.listener;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.message.relationship.FriendConnectionMessage;
import dev.emortal.velocity.messaging.MessagingCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FriendConnectionListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String FRIEND_CONNECT_MESSAGE = "<green>Friend > <username> has connected";
    private static final String FRIEND_DISCONNECT_MESSAGE = "<green>Friend > <username> has disconnected";

    public FriendConnectionListener(@NotNull ProxyServer proxy, @NotNull MessagingCore messaging) {
        messaging.addListener(FriendConnectionMessage.class, message -> {
            for (String targetIdStr : message.getMessageTargetIdsList()) {
                UUID targetId = UUID.fromString(targetIdStr);
                proxy.getPlayer(targetId).ifPresent(target -> {
                    TagResolver tagResolver = Placeholder.unparsed("username", message.getUsername());
                    if (message.getJoined()) {
                        target.sendMessage(MINI_MESSAGE.deserialize(FRIEND_CONNECT_MESSAGE, tagResolver));
                    } else {
                        target.sendMessage(MINI_MESSAGE.deserialize(FRIEND_DISCONNECT_MESSAGE, tagResolver));
                    }
                });
            }
        });
    }
}
