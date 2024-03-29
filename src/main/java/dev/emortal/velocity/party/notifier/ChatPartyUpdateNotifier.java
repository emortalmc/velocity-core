package dev.emortal.velocity.party.notifier;

import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.party.Party;
import dev.emortal.api.model.party.PartyMember;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.party.cache.LocalParty;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class ChatPartyUpdateNotifier implements PartyUpdateNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatPartyUpdateNotifier.class);

    private final @NotNull PlayerProvider playerProvider;

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
        // we don't broadcast open state changed
    }

    @Override
    public void partyLeaderChanged(@NotNull LocalParty party, @NotNull String newLeaderName) {
        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            ChatMessages.PARTY_LEADER_CHANGED.send(member, newLeaderName);
        }
    }

    @Override
    public void partyInviteCreated(@NotNull LocalParty party, @NotNull UUID senderId, @NotNull String senderName,
                                   @NotNull String targetName) {
        for (UUID memberId : party.memberIds()) {
            if (memberId.equals(senderId)) continue; // Don't send a notification to the sender

            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            ChatMessages.PLAYER_INVITED_TO_PARTY.send(member, senderName, targetName);
        }
    }

    @Override
    public void selfInvited(@NotNull UUID targetId, @NotNull String senderName) {
        Player player = this.playerProvider.getPlayer(targetId);
        if (player == null) return;

        ChatMessages.YOU_INVITED_TO_PARTY.send(player, senderName);
    }

    @Override
    public void playerJoined(@NotNull LocalParty party, @NotNull String joinerName) {
        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            ChatMessages.PLAYER_JOINED_PARTY.send(member, joinerName);
        }
    }

    @Override
    public void playerLeft(@NotNull LocalParty party, @NotNull String leaverName) {
        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            ChatMessages.PLAYER_LEFT_PARTY.send(member, leaverName);
        }
    }

    @Override
    public void playerKicked(@NotNull LocalParty party, @NotNull String targetName, @NotNull String kickerName) {
        for (UUID memberId : party.memberIds()) {
            Player member = this.playerProvider.getPlayer(memberId);
            if (member == null) continue;

            ChatMessages.PLAYER_KICKED_FROM_PARTY.send(member, targetName, kickerName);
        }
    }

    @Override
    public void selfKicked(@NotNull UUID targetId, @NotNull String kickerName) {
        Player target = this.playerProvider.getPlayer(targetId);
        if (target == null) return;

        ChatMessages.YOU_KICKED_FROM_PARTY.send(target, kickerName);
    }

    @Override
    public void partyBroadcast(@NotNull Party party) {
        String leaderId = party.getLeaderId();
        PartyMember leader = party.getMembersList().stream()
                .filter(member -> member.getId().equals(leaderId))
                .findFirst().orElse(null);

        if (leader == null) {
            LOGGER.warn("Couldn't find leader for party {}", party.getId());
            return;
        }

        this.playerProvider.allPlayers().forEach(player -> ChatMessages.PARTY_BROADCAST_MESSAGE.send(player, leader.getUsername()));
    }
}
