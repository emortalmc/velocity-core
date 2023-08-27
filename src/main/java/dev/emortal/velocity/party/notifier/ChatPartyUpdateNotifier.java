package dev.emortal.velocity.party.notifier;

import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.party.cache.LocalParty;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ChatPartyUpdateNotifier implements PartyUpdateNotifier {

    private final PlayerProvider playerProvider;

    public ChatPartyUpdateNotifier(@NotNull PlayerProvider playerProvider) {
        this.playerProvider = playerProvider;
    }

    @Override
    public void partyEmptied(@NotNull UUID memberId) {
        Player player = this.playerProvider.getPlayer(memberId);
        if (player != null) ChatMessages.PARTY_DISBANDED.send(player);
    }

    @Override
    public void partyDeleted(@NotNull UUID memberId) {
        this.partyEmptied(memberId);
    }

    @Override
    public void partyOpenStateChanged(@NotNull LocalParty party, boolean open) {
        ChatMessages message = open ? ChatMessages.YOU_OPENED_PARTY : ChatMessages.YOU_CLOSED_PARTY;

        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            message.send(member);
        }
    }

    @Override
    public void partyLeaderChanged(@NotNull LocalParty party, @NotNull String newLeaderName) {
        Component usernameArgument = Component.text(newLeaderName);

        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            ChatMessages.PARTY_LEADER_CHANGED.send(member, usernameArgument);
        }
    }

    @Override
    public void partyInviteCreated(@NotNull LocalParty party, @NotNull UUID senderId, @NotNull String senderName,
                                   @NotNull String targetName) {
        Component usernameArgument = Component.text(targetName);
        Component senderNameArgument = Component.text(senderName);

        for (UUID memberId : party.memberIds()) {
            if (memberId.equals(senderId)) continue; // Don't send a notification to the sender

            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            ChatMessages.PLAYER_INVITED_TO_PARTY.send(member, usernameArgument, senderNameArgument);
        }
    }

    @Override
    public void selfInvited(@NotNull UUID targetId, @NotNull String senderName) {
        Player player = this.playerProvider.getPlayer(targetId);
        if (player == null) return;

        ChatMessages.YOU_INVITED_PLAYER_TO_PARTY.send(player, Component.text(senderName));
    }

    @Override
    public void playerJoined(@NotNull LocalParty party, @NotNull String joinerName) {
        Component username = Component.text(joinerName);

        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            ChatMessages.PLAYER_JOINED_PARTY.send(member, username);
        }
    }

    @Override
    public void playerLeft(@NotNull LocalParty party, @NotNull String leaverName) {
        Component username = Component.text(leaverName);

        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            ChatMessages.PLAYER_LEFT_PARTY.send(member, username);
        }
    }

    @Override
    public void playerKicked(@NotNull LocalParty party, @NotNull String targetName, @NotNull String kickerName) {
        Component username = Component.text(targetName);
        Component kicker = Component.text(kickerName);

        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            ChatMessages.PLAYER_KICKED_FROM_PARTY.send(member, username, kicker);
        }
    }

    @Override
    public void selfKicked(@NotNull UUID targetId, @NotNull String kickerName) {
        Player target = this.playerProvider.getPlayer(targetId);
        if (target == null) return;

        ChatMessages.YOU_KICKED_PLAYER_FROM_PARTY.send(target, Component.text(kickerName));
    }
}
