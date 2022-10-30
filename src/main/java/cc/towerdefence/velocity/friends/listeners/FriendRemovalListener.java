package cc.towerdefence.velocity.friends.listeners;

import cc.towerdefence.velocity.api.event.friend.FriendRemoveReceivedEvent;
import cc.towerdefence.velocity.friends.FriendCache;
import com.velocitypowered.api.event.Subscribe;

public class FriendRemovalListener {
    private final FriendCache friendCache;

    public FriendRemovalListener(FriendCache friendCache) {
        this.friendCache = friendCache;
    }

    @Subscribe
    public void onFriendRemoveReceived(FriendRemoveReceivedEvent event) {
        this.friendCache.remove(event.recipientId(), event.senderId());
        // don't notify the user
    }
}
