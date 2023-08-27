package dev.emortal.velocity.party;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
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
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.messaging.MessagingModule;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the parties of users logged into the proxy.
 * Maps a user's UUID to their {@link dev.emortal.api.model.party.Party} and a party's ObjectID to the party.
 */
public final class PartyCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyCache.class);

    private final @NotNull PartyService partyService;
    private final @NotNull ProxyServer proxy;

    private final @NotNull Map<String, CachedParty> partyMap = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, CachedParty> playerPartyMap = new ConcurrentHashMap<>();

    public PartyCache(@NotNull ProxyServer proxy, @NotNull PartyService partyService, @NotNull MessagingModule messagingCore) {
        this.proxy = proxy;
        this.partyService = partyService;

        messagingCore.addListener(PartyCreatedMessage.class, this::handleCreateParty);
        messagingCore.addListener(PartyDeletedMessage.class, this::handlePartyDeleted);
        messagingCore.addListener(PartyEmptiedMessage.class, this::handlePartyEmptied);
        messagingCore.addListener(PartyOpenChangedMessage.class, this::handlePartyOpenChanged);
        messagingCore.addListener(PartyPlayerJoinedMessage.class, this::handleJoinParty);
        messagingCore.addListener(PartyPlayerLeftMessage.class, this::handleLeaveParty);
        messagingCore.addListener(PartyLeaderChangedMessage.class, this::handleLeaderChange);
        messagingCore.addListener(PartyInviteCreatedMessage.class, this::handleInviteCreated);
    }

    public @Nullable CachedParty getParty(@NotNull String partyId) {
        return this.partyMap.get(partyId);
    }

    public @Nullable CachedParty getPlayerParty(@NotNull UUID playerId) {
        return this.playerPartyMap.get(playerId);
    }

    private void handleCreateParty(@NotNull PartyCreatedMessage message) {
        Party party = message.getParty();
        UUID partyLeaderId = UUID.fromString(party.getLeaderId());

        Player player = this.proxy.getPlayer(partyLeaderId).orElse(null);
        if (player == null) return;

        CachedParty cachedParty = CachedParty.fromProto(party);
        this.playerPartyMap.put(player.getUniqueId(), cachedParty);
        this.partyMap.put(party.getId(), cachedParty);
    }

    private void handlePartyEmptied(@NotNull PartyEmptiedMessage message) {
        Party party = message.getParty();

        CachedParty cachedParty = this.partyMap.get(party.getId());
        if (cachedParty == null) return;

        for (PartyMember member : party.getMembersList()) {
            // Leave the leader in the party.
            if (member.getId().equals(party.getLeaderId())) continue;

            UUID memberId = UUID.fromString(member.getId());
            cachedParty.getMembers().remove(memberId);

            CachedParty playerRemoved = this.playerPartyMap.remove(memberId);
            if (playerRemoved != null) {
                this.proxy.getPlayer(memberId).ifPresent(ChatMessages.PARTY_DISBANDED::send);
            }
        }

        // When a party is emptied, its state is reset, so it becomes closed.
        cachedParty.setOpen(false);
    }

    private void handlePartyOpenChanged(@NotNull PartyOpenChangedMessage message) {
        String partyId = message.getPartyId();

        CachedParty party = this.partyMap.get(partyId);
        if (party == null) return;

        party.setOpen(message.getOpen());
    }

    private void handlePartyDeleted(@NotNull PartyDeletedMessage message) {
        Party party = message.getParty();

        CachedParty removed = this.partyMap.remove(party.getId());
        if (removed == null) return;

        int remainingMembers = party.getMembersCount();
        if (remainingMembers <= 1) return; // Don't send any notifications if the party was just themselves.

        for (PartyMember member : party.getMembersList()) {
            UUID memberId = UUID.fromString(member.getId());
            CachedParty playerRemoved = this.playerPartyMap.remove(memberId);

            if (playerRemoved != null) {
                this.proxy.getPlayer(memberId).ifPresent(ChatMessages.PARTY_DISBANDED::send);
            }

            // Just a fast finish optimisation
            if (--remainingMembers == 0) break;
        }
    }

    private void handleJoinParty(@NotNull PartyPlayerJoinedMessage message) {
        String partyId = message.getPartyId();
        UUID playerId = UUID.fromString(message.getMember().getId());

        Player player = this.proxy.getPlayer(playerId).orElse(null);
        if (player == null) return;

        CachedParty party = this.cachePartyIfNotPresent(partyId);
        if (party == null) return;

        for (UUID memberId : party.getMembers().keySet()) {
            Player member = this.proxy.getPlayer(memberId).orElse(null);
            if (member == null) continue;

            ChatMessages.PLAYER_JOINED_PARTY.send(member, Component.text(player.getUsername()));
        }

        this.playerPartyMap.put(playerId, this.partyMap.get(partyId));
        party.getMembers().put(playerId, CachedPartyMember.fromProto(message.getMember()));
    }

    private void handleLeaveParty(@NotNull PartyPlayerLeftMessage message) {
        String partyId = message.getPartyId();
        UUID playerId = UUID.fromString(message.getMember().getId());

        boolean isKick = message.hasKickedBy();

        CachedParty party = this.getParty(partyId);
        if (party != null) {
            party.getMembers().remove(playerId);

            // Handle party notifications
            for (UUID memberId : party.getMembers().keySet()) {
                Player member = this.proxy.getPlayer(memberId).orElse(null);
                if (member == null) continue;

                Component username = Component.text(message.getMember().getUsername());
                if (isKick) {
                    ChatMessages.PLAYER_KICKED_FROM_PARTY.send(member, username, Component.text(message.getKickedBy().getUsername()));
                } else {
                    ChatMessages.PLAYER_LEFT_PARTY.send(member, username);
                }
            }
        }

        // Handle target notification
        Player target = this.proxy.getPlayer(playerId).orElse(null);
        if (target == null) return;

        if (isKick) {
            ChatMessages.YOU_KICKED_FROM_PARTY.send(target, Component.text(message.getKickedBy().getUsername()));
        }
        // don't need to send a message if they're not kicked as they chose to leave
    }

    private void handleLeaderChange(@NotNull PartyLeaderChangedMessage message) {
        String partyId = message.getPartyId();
        UUID newLeaderId = UUID.fromString(message.getNewLeader().getId());

        CachedParty party = this.partyMap.get(partyId);
        if (party == null) return;

        for (UUID memberId : party.getMembers().keySet()) {
            Player member = this.proxy.getPlayer(memberId).orElse(null);
            if (member == null) continue;

            ChatMessages.PARTY_LEADER_CHANGED.send(member, Component.text(message.getNewLeader().getUsername()));
        }

        party.setLeaderId(newLeaderId);
    }

    /**
     * This method is a notification only method. Party invites are not cached.
     *
     * @param message The invite message
     */
    private void handleInviteCreated(@NotNull PartyInviteCreatedMessage message) {
        PartyInvite invite = message.getInvite();
        Component senderName = Component.text(invite.getSenderUsername());

        // Notify the party members of the invite
        CachedParty party = this.cachePartyIfNotPresent(invite.getPartyId());
        if (party != null) {
            UUID senderId = UUID.fromString(invite.getSenderId());
            Component targetName = Component.text(invite.getTargetUsername());

            for (UUID memberId : party.getMembers().keySet()) {
                if (memberId.equals(senderId)) continue; // Don't send a notification to the sender

                Player member = this.proxy.getPlayer(memberId).orElse(null);
                if (member == null) continue;

                ChatMessages.PLAYER_INVITED_TO_PARTY.send(member, senderName, targetName);
            }
        }

        // Notify the invited player
        Player player = this.proxy.getPlayer(UUID.fromString(invite.getTargetId())).orElse(null);
        if (player == null) return;

        ChatMessages.YOU_INVITED_TO_PARTY.send(player, senderName);
    }

    /**
     * Gets the party by its id.
     * WARNING: This method is blocking.
     *
     * @param partyId The id of the party
     */
    private @Nullable CachedParty cachePartyIfNotPresent(@NotNull String partyId) {
        CachedParty cachedParty = this.partyMap.get(partyId);
        if (cachedParty != null) return cachedParty;

        Party party;
        try {
            party = this.partyService.getParty(partyId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get party", exception);
            return null;
        }

        cachedParty = CachedParty.fromProto(party);
        this.partyMap.put(party.getId(), cachedParty);

        return cachedParty;
    }

    public static final class CachedParty {

        public static @NotNull CachedParty fromProto(@NotNull Party party) {
            Map<UUID, CachedPartyMember> members = new ConcurrentHashMap<>();
            for (PartyMember member : party.getMembersList()) {
                UUID memberId = UUID.fromString(member.getId());
                members.put(memberId, CachedPartyMember.fromProto(member));
            }

            return new CachedParty(party.getId(), UUID.fromString(party.getLeaderId()), members, party.getOpen());
        }

        private final @NotNull String id;
        private final @NotNull Map<UUID, CachedPartyMember> members;
        private @NotNull UUID leaderId;
        private boolean open;

        public CachedParty(@NotNull String id, @NotNull UUID leaderId, @NotNull Map<UUID, CachedPartyMember> members, boolean open) {
            this.id = id;
            this.members = members;
            this.leaderId = leaderId;
            this.open = open;
        }

        public @NotNull String getId() {
            return this.id;
        }

        public @NotNull Map<UUID, CachedPartyMember> getMembers() {
            return this.members;
        }

        public @NotNull UUID getLeaderId() {
            return this.leaderId;
        }

        public void setLeaderId(@NotNull UUID leaderId) {
            this.leaderId = leaderId;
        }

        public boolean isOpen() {
            return this.open;
        }

        public void setOpen(boolean open) {
            this.open = open;
        }

        @Override
        public String toString() {
            return "CachedParty{" +
                    "id='" + this.id + '\'' +
                    ", members=" + this.members +
                    ", leaderId=" + this.leaderId +
                    ", open=" + this.open +
                    '}';
        }
    }

    public record CachedPartyMember(@NotNull UUID id, @NotNull String username) {

        public static @NotNull CachedPartyMember fromProto(@NotNull PartyMember member) {
            return new CachedPartyMember(UUID.fromString(member.getId()), member.getUsername());
        }
    }
}
