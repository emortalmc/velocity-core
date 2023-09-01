package dev.emortal.velocity.party;

import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.message.party.PartyCreatedMessage;
import dev.emortal.api.message.party.PartyDeletedMessage;
import dev.emortal.api.message.party.PartyEmptiedMessage;
import dev.emortal.api.message.party.PartyInviteCreatedMessage;
import dev.emortal.api.message.party.PartyLeaderChangedMessage;
import dev.emortal.api.message.party.PartyOpenChangedMessage;
import dev.emortal.api.message.party.PartyPlayerJoinedMessage;
import dev.emortal.api.message.party.PartyPlayerLeftMessage;
import dev.emortal.api.model.party.Party;
import dev.emortal.api.model.party.PartyInvite;
import dev.emortal.api.model.party.PartyMember;
import dev.emortal.velocity.messaging.MessageHandler;
import dev.emortal.velocity.party.notifier.PartyUpdateNotifier;
import dev.emortal.velocity.adapter.player.PlayerProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

final class PartyUpdateListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyUpdateListener.class);

    private final @NotNull PartyCache partyCache;
    private final @NotNull PlayerProvider playerProvider;
    private final @NotNull PartyUpdateNotifier updateNotifier;

    PartyUpdateListener(@NotNull PartyCache partyCache, @NotNull PlayerProvider playerProvider, @NotNull PartyUpdateNotifier updateNotifier,
                        @NotNull MessageHandler messaging) {
        this.partyCache = partyCache;
        this.playerProvider = playerProvider;
        this.updateNotifier = updateNotifier;

        messaging.addListener(PartyCreatedMessage.class, this::handleCreateParty);
        messaging.addListener(PartyDeletedMessage.class, this::handlePartyDeleted);
        messaging.addListener(PartyEmptiedMessage.class, this::handlePartyEmptied);
        messaging.addListener(PartyOpenChangedMessage.class, this::handlePartyOpenChanged);
        messaging.addListener(PartyPlayerJoinedMessage.class, this::handleJoinParty);
        messaging.addListener(PartyPlayerLeftMessage.class, this::handleLeaveParty);
        messaging.addListener(PartyLeaderChangedMessage.class, this::handleLeaderChange);
        messaging.addListener(PartyInviteCreatedMessage.class, this::handleInviteCreated);
    }

    void handleCreateParty(@NotNull PartyCreatedMessage message) {
        Party party = message.getParty();
        UUID partyLeaderId = UUID.fromString(party.getLeaderId());

        Player player = this.playerProvider.getPlayer(partyLeaderId);
        if (player == null) return;

        this.partyCache.cacheParty(party);
    }

    void handlePartyEmptied(@NotNull PartyEmptiedMessage message) {
        Party party = message.getParty();

        PartyCache.CachedParty cachedParty = this.partyCache.getParty(party.getId());
        if (cachedParty == null) {
            LOGGER.warn("Attempted to empty party {} that was not cached", party.getId());
            return;
        }

        for (PartyMember member : party.getMembersList()) {
            // Leave the leader in the party.
            if (member.getId().equals(party.getLeaderId())) continue;

            UUID memberId = UUID.fromString(member.getId());
            cachedParty.removeMember(memberId);

            this.updateNotifier.partyEmptied(memberId);
        }

        // When a party is emptied, its state is reset, so it becomes closed.
        cachedParty.setOpen(false);
        this.updateNotifier.partyOpenStateChanged(cachedParty, false);
    }

    void handlePartyOpenChanged(@NotNull PartyOpenChangedMessage message) {
        String partyId = message.getPartyId();

        PartyCache.CachedParty party = this.partyCache.getParty(partyId);
        if (party == null) {
            LOGGER.warn("Attempted to change open state of party {} that was not cached", partyId);
            return;
        }

        party.setOpen(message.getOpen());
        this.updateNotifier.partyOpenStateChanged(party, message.getOpen());
    }

    void handlePartyDeleted(@NotNull PartyDeletedMessage message) {
        Party party = message.getParty();

        PartyCache.CachedParty removed = this.partyCache.removeParty(party.getId());
        if (removed == null) {
            LOGGER.warn("Attempted to delete party {} that was not cached", party.getId());
            return;
        }
        if (party.getMembersCount() <= 1) return; // Don't send any notifications if the party was just themselves.

        for (PartyMember member : party.getMembersList()) {
            UUID memberId = UUID.fromString(member.getId());

            this.updateNotifier.partyDeleted(memberId);
        }
    }

    void handleJoinParty(@NotNull PartyPlayerJoinedMessage message) {
        String partyId = message.getPartyId();
        UUID playerId = UUID.fromString(message.getMember().getId());

        Player player = this.playerProvider.getPlayer(playerId);
        if (player == null) return;

        PartyCache.CachedParty party = this.partyCache.cachePartyIfNotPresent(partyId);
        if (party == null) return;

        this.updateNotifier.playerJoined(party, player.getUsername());
        party.addMember(playerId, message.getMember());
    }

    void handleLeaveParty(@NotNull PartyPlayerLeftMessage message) {
        String partyId = message.getPartyId();
        UUID playerId = UUID.fromString(message.getMember().getId());

        boolean isKick = message.hasKickedBy();
        if (isKick) this.updateNotifier.selfKicked(playerId, message.getKickedBy().getUsername());

        PartyCache.CachedParty party = this.partyCache.getParty(partyId);
        if (party == null) {
            LOGGER.warn("Attempted to remove player {} from party {} that was not cached", message.getMember().getUsername(), partyId);
            return;
        }

        party.removeMember(playerId);
        String leaverName = message.getMember().getUsername();

        if (isKick) {
            this.updateNotifier.playerKicked(party, leaverName, message.getKickedBy().getUsername());
        } else {
            this.updateNotifier.playerLeft(party, leaverName);
        }
    }

    void handleLeaderChange(@NotNull PartyLeaderChangedMessage message) {
        String partyId = message.getPartyId();
        UUID newLeaderId = UUID.fromString(message.getNewLeader().getId());

        PartyCache.CachedParty party = this.partyCache.getParty(partyId);
        if (party == null) {
            LOGGER.warn("Attempted to change leader for party {} that was not cached", partyId);
            return;
        }

        party.setLeaderId(newLeaderId);
        this.updateNotifier.partyLeaderChanged(party, message.getNewLeader().getUsername());
    }

    /**
     * This method is a notification only method. Party invites are not cached.
     *
     * @param message The invite message
     */
    void handleInviteCreated(@NotNull PartyInviteCreatedMessage message) {
        PartyInvite invite = message.getInvite();
        this.updateNotifier.selfInvited(UUID.fromString(invite.getTargetId()), invite.getSenderUsername());

        PartyCache.CachedParty party = this.partyCache.cachePartyIfNotPresent(invite.getPartyId());
        if (party == null) {
            LOGGER.warn("Attempted to invite {} to party {} that was not cached", invite.getTargetUsername(), invite.getPartyId());
            return;
        }

        UUID senderId = UUID.fromString(invite.getSenderId());
        String targetUsername = invite.getTargetUsername();

        this.updateNotifier.partyInviteCreated(party, senderId, invite.getSenderUsername(), targetUsername);
    }
}
