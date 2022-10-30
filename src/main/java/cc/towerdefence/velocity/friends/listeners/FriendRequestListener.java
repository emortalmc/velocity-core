package cc.towerdefence.velocity.friends.listeners;

import cc.towerdefence.velocity.api.event.friend.FriendRequestReceivedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class FriendRequestListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String FRIEND_REQUEST_RECEIVED_MESSAGE = "<light_purple>You have received a friend request from <color:#c98fff><sender_username></color> <click:run_command:'/friend add <sender_username>'><green>ACCEPT</click> <reset><gray>| <click:run_command:'/friend deny <sender_username>'><red>DENY</click>";

    private final ProxyServer proxy;

    public FriendRequestListener(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onFriendRequestReceived(FriendRequestReceivedEvent event) {
        this.proxy.getPlayer(event.recipientId()).ifPresent(player -> {
            player.sendMessage(MINI_MESSAGE.deserialize(FRIEND_REQUEST_RECEIVED_MESSAGE, Placeholder.parsed("sender_username", event.senderUsername())));
        });
    }
}
