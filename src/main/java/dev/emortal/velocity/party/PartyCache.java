package dev.emortal.velocity.party;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.grpc.party.PartyProto;
import dev.emortal.api.grpc.party.PartyServiceGrpc;
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
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.rabbitmq.RabbitMqCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the parties of users logged into the proxy.
 * Maps a user's UUID to their {@link dev.emortal.api.model.party.Party} and a party's ObjectID to the party.
 */
public class PartyCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyCache.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component NOTIFICATION_PARTY_DISBANDED = MINI_MESSAGE.deserialize("The party you were in has been disbanded");
    private static final String NOTIFICATION_PARTY_PLAYER_JOINED = "<username> has joined the party";

    private static final String NOTIFICATION_PARTY_PLAYER_LEFT = "<username> has left the party";
    private static final String NOTIFICATION_PARTY_KICKED = "<username> has been kicked from the party by <kicker>";
    private static final String NOTIFICATION_PLAYER_KICKED = "You have been kicked from the party by <kicker>";

    private static final String NOTIFICATION_PARTY_LEADER_CHANGED = "<username> is now the party leader";

    private static final String NOTIFICATION_PARTY_INVITE_CREATED = "<username> has been invited to the party";
    private static final String NOTIFICATION_PLAYER_INVITE_CREATED = "You have been invited to join <username>'s party. Click <click:run_command:'/party join <username>'>HERE</click> to accept";

    private final @NotNull Map<UUID, CachedParty> playerPartyMap = new ConcurrentHashMap<>();
    private final @NotNull Map<String, CachedParty> partyMap = new ConcurrentHashMap<>();

    private final PartyServiceGrpc.PartyServiceFutureStub partyService = GrpcStubCollection.getPartyService().orElse(null);
    private final @NotNull RabbitMqCore rabbitMqCore;
    private final @NotNull ProxyServer proxy;

    public PartyCache(@NotNull ProxyServer proxy, @NotNull RabbitMqCore rabbitMqCore) {
        this.rabbitMqCore = rabbitMqCore;
        this.proxy = proxy;

        this.rabbitMqCore.setListener(PartyCreatedMessage.class, this::handleCreateParty);
        this.rabbitMqCore.setListener(PartyDeletedMessage.class, this::handlePartyDeleted);
        this.rabbitMqCore.setListener(PartyEmptiedMessage.class, this::handlePartyEmptied);
        this.rabbitMqCore.setListener(PartyOpenChangedMessage.class, this::handlePartyOpenChanged);
        this.rabbitMqCore.setListener(PartyPlayerJoinedMessage.class, this::handleJoinParty);
        this.rabbitMqCore.setListener(PartyPlayerLeftMessage.class, this::handleLeaveParty);
        this.rabbitMqCore.setListener(PartyLeaderChangedMessage.class, this::handleLeaderChange);
        this.rabbitMqCore.setListener(PartyInviteCreatedMessage.class, this::handleInviteCreated);
    }

    public CachedParty getPlayerParty(@NotNull UUID playerId) {
        return this.playerPartyMap.get(playerId);
    }

    public CachedParty getParty(@NotNull String partyId) {
        return this.partyMap.get(partyId);
    }

    private void handleCreateParty(@NotNull PartyCreatedMessage message) {
        Party party = message.getParty();
        UUID partyLeaderId = UUID.fromString(party.getLeaderId());

        this.proxy.getPlayer(partyLeaderId).ifPresent(player -> {
            CachedParty cachedParty = CachedParty.fromProto(party);
            this.playerPartyMap.put(player.getUniqueId(), cachedParty);
            this.partyMap.put(party.getId(), cachedParty);
        });
    }

    private void handlePartyEmptied(@NotNull PartyEmptiedMessage message) {
        Party party = message.getParty();

        CachedParty cachedParty = this.partyMap.get(party.getId());
        if (cachedParty == null) return;

        for (PartyMember member : party.getMembersList()) {
            // Leave the leader in the party.
            if (member.getId().equals(party.getLeaderId())) continue;
            UUID memberId = UUID.fromString(member.getId());
            CachedParty playerRemoved = this.playerPartyMap.remove(memberId);
            cachedParty.getMembers().remove(memberId);

            if (playerRemoved != null) {
                this.proxy.getPlayer(memberId).ifPresent(player -> player.sendMessage(NOTIFICATION_PARTY_DISBANDED));
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
        if (removed == null) {
            return;
        }

        int remainingMembers = party.getMembersCount();
        for (PartyMember member : party.getMembersList()) {
            UUID memberId = UUID.fromString(member.getId());
            CachedParty playerRemoved = this.playerPartyMap.remove(memberId);

            if (playerRemoved != null) {
                this.proxy.getPlayer(memberId).ifPresent(player -> player.sendMessage(NOTIFICATION_PARTY_DISBANDED));
            }

            // Just a fast finish optimisation
            if (--remainingMembers == 0) {
                break;
            }
        }
    }

    private void handleJoinParty(@NotNull PartyPlayerJoinedMessage message) {
        String partyId = message.getPartyId();
        UUID playerId = UUID.fromString(message.getMember().getId());

        this.proxy.getPlayer(playerId).ifPresent(player -> {
            CachedParty party = this.cachePartyIfNotPresent(partyId);
            if (party == null) {
                return;
            }

            for (UUID memberId : party.getMembers().keySet()) {
                this.proxy.getPlayer(memberId).ifPresent(memberPlayer -> memberPlayer
                        .sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PARTY_PLAYER_JOINED, Placeholder.unparsed("username", player.getUsername()))));
            }

            this.playerPartyMap.put(playerId, this.partyMap.get(partyId));
            party.getMembers().put(playerId, CachedPartyMember.fromProto(message.getMember()));
        });
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
                this.proxy.getPlayer(memberId).ifPresent(memberPlayer -> {
                    if (isKick) {
                        memberPlayer.sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PARTY_KICKED, Placeholder.unparsed("username", message.getMember().getUsername()), Placeholder.unparsed("kicker", message.getKickedBy().getUsername())));
                    } else {
                        memberPlayer.sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PARTY_PLAYER_LEFT, Placeholder.unparsed("username", message.getMember().getUsername())));
                    }
                });
            }
        }

        // Handle target notification
        this.proxy.getPlayer(playerId).ifPresent(targetPlayer -> {
            if (isKick) {
                targetPlayer.sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PLAYER_KICKED, Placeholder.unparsed("kicker", message.getKickedBy().getUsername())));
            }
            // don't need to send a message if they're not kicked as they chose to leave
        });
    }

    private void handleLeaderChange(@NotNull PartyLeaderChangedMessage message) {
        String partyId = message.getPartyId();
        UUID newLeaderId = UUID.fromString(message.getNewLeader().getId());

        CachedParty party = this.partyMap.get(partyId);
        if (party == null) {
            return;
        }
        for (UUID memberId : party.getMembers().keySet()) {
            this.proxy.getPlayer(memberId).ifPresent(player -> player
                    .sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PARTY_LEADER_CHANGED, Placeholder.unparsed("username", message.getNewLeader().getUsername()))));
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

        // Notify the party members of the invite
        CachedParty party = this.cachePartyIfNotPresent(invite.getPartyId());
        if (party != null) {
            for (UUID memberId : party.getMembers().keySet()) {
                this.proxy.getPlayer(memberId).ifPresent(player -> player
                        .sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PARTY_INVITE_CREATED, Placeholder.parsed("username", invite.getSenderUsername()))));
            }
        }

        // Notify the invited player
        this.proxy.getPlayer(UUID.fromString(invite.getTargetId())).ifPresent(player -> player
                .sendMessage(MINI_MESSAGE.deserialize(NOTIFICATION_PLAYER_INVITE_CREATED, Placeholder.parsed("username", invite.getSenderUsername()))));
    }

    /**
     * Gets the party by its id.
     * WARNING: This method is blocking.
     *
     * @param partyId The id of the party
     */
    private CachedParty cachePartyIfNotPresent(@NotNull String partyId) {
        CachedParty cachedParty = this.partyMap.get(partyId);
        if (cachedParty != null) {
            return cachedParty;
        }

        var getPartyFuture = this.partyService.getParty(PartyProto.GetPartyRequest.newBuilder()
                .setPartyId(partyId)
                .build());

        PartyProto.GetPartyResponse response;
        try {
            response = getPartyFuture.get();
        } catch (Exception e) {
            LOGGER.error("Failed to get party", e);
            return null;
        }

        Party party = response.getParty();
        cachedParty = CachedParty.fromProto(party);
        this.partyMap.put(party.getId(), cachedParty);

        return cachedParty;
    }

    public static class CachedParty {
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

        public static CachedParty fromProto(@NotNull Party party) {
            Map<UUID, CachedPartyMember> members = new ConcurrentHashMap<>();
            for (PartyMember member : party.getMembersList()) {
                UUID memberId = UUID.fromString(member.getId());
                members.put(memberId, CachedPartyMember.fromProto(member));
            }

            return new CachedParty(party.getId(), UUID.fromString(party.getLeaderId()), members, party.getOpen());
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

        public void setOpen(boolean open) {
            this.open = open;
        }

        public boolean isOpen() {
            return this.open;
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

    public record CachedPartyMember(UUID id, String username) {

        public static @NotNull CachedPartyMember fromProto(PartyMember member) {
            return new CachedPartyMember(UUID.fromString(member.getId()), member.getUsername());
        }
    }
}
