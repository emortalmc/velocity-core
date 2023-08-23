package dev.emortal.velocity.party.notifier;

import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.party.cache.LocalParty;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ChatPartyUpdateNotifier implements PartyUpdateNotifier {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component NOTIFICATION_PARTY_DISBANDED = MINI_MESSAGE.deserialize("<red>The party you were in has been disbanded");
    private static final String NOTIFICATION_PARTY_PLAYER_JOINED = "<green><username> has joined the party";

    private static final String NOTIFICATION_PARTY_PLAYER_LEFT = "<red><username> has left the party";
    private static final String NOTIFICATION_PARTY_KICKED = "<username> has been kicked from the party by <kicker>";
    private static final String NOTIFICATION_PLAYER_KICKED = "<red>You have been kicked from the party by <kicker>";

    private static final String NOTIFICATION_PARTY_LEADER_CHANGED = "<username> is now the party leader";
    private static final Component NOTIFICATION_PARTY_OPENED = Component.text("The party is now open", NamedTextColor.GREEN);
    private static final Component NOTIFICATION_PARTY_CLOSED = Component.text("The party is now closed", NamedTextColor.GREEN);

    private static final String NOTIFICATION_PARTY_INVITE_CREATED_MEMBERS = "<sender_username> has invited <username> to the party";
    private static final String NOTIFICATION_PLAYER_INVITE_CREATED = "<click:run_command:'/party join <username>'><color:#3db83d>You have been invited to join <green><username>'s</green> party. <b><gradient:light_purple:gold>Click to accept</gradient></b></click>";

    private final PlayerProvider playerProvider;

    public ChatPartyUpdateNotifier(@NotNull PlayerProvider playerProvider) {
        this.playerProvider = playerProvider;
    }

    @Override
    public void partyEmptied(@NotNull UUID memberId) {
        Player player = this.playerProvider.getPlayer(memberId);
        if (player != null) player.sendMessage(NOTIFICATION_PARTY_DISBANDED);
    }

    @Override
    public void partyDeleted(@NotNull UUID memberId) {
        this.partyEmptied(memberId);
    }

    @Override
    public void partyOpenStateChanged(@NotNull LocalParty party, boolean open) {
        Component message = open ? NOTIFICATION_PARTY_OPENED : NOTIFICATION_PARTY_CLOSED;

        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            member.sendMessage(message);
        }
    }

    @Override
    public void partyLeaderChanged(@NotNull LocalParty party, @NotNull String newLeaderName) {
        var usernamePlaceholder = Placeholder.unparsed("username", newLeaderName);

        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            member.sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PARTY_LEADER_CHANGED, usernamePlaceholder));
        }
    }

    @Override
    public void partyInviteCreated(@NotNull LocalParty party, @NotNull UUID senderId, @NotNull String senderName,
                                   @NotNull String targetName) {
        var usernamePlaceholder = Placeholder.unparsed("username", targetName);
        var senderNamePlaceholder = Placeholder.unparsed("sender_username", senderName);

        for (UUID memberId : party.memberIds()) {
            if (memberId.equals(senderId)) continue; // Don't send a notification to the sender

            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            member.sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PARTY_INVITE_CREATED_MEMBERS, usernamePlaceholder, senderNamePlaceholder));
        }
    }

    @Override
    public void selfInvited(@NotNull UUID targetId, @NotNull String senderName) {
        Player player = this.playerProvider.getPlayer(targetId);
        if (player == null) return;

        player.sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PLAYER_INVITE_CREATED, Placeholder.parsed("username", senderName)));
    }

    @Override
    public void playerJoined(@NotNull LocalParty party, @NotNull String joinerName) {
        var usernamePlaceholder = Placeholder.unparsed("username", joinerName);

        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            member.sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PARTY_PLAYER_JOINED, usernamePlaceholder));
        }
    }

    @Override
    public void playerLeft(@NotNull LocalParty party, @NotNull String leaverName) {
        var usernamePlaceholder = Placeholder.unparsed("username", leaverName);

        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            member.sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PARTY_PLAYER_LEFT, usernamePlaceholder));
        }
    }

    @Override
    public void playerKicked(@NotNull LocalParty party, @NotNull String targetName, @NotNull String kickerName) {
        var usernamePlaceholder = Placeholder.unparsed("username", targetName);
        var kickerPlaceholder = Placeholder.unparsed("kicker", kickerName);

        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            member.sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PARTY_KICKED, usernamePlaceholder, kickerPlaceholder));
        }
    }

    @Override
    public void selfKicked(@NotNull UUID targetId, @NotNull String kickerName) {
        Player target = this.playerProvider.getPlayer(targetId);
        if (target == null) return;

        target.sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PLAYER_KICKED, Placeholder.unparsed("kicker", kickerName)));
    }
}
