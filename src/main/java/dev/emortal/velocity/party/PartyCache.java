package dev.emortal.velocity.party;

import dev.emortal.api.model.party.Party;
import dev.emortal.api.model.party.PartyMember;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.party.cache.LocalParty;
import dev.emortal.velocity.party.cache.LocalPartyMember;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the parties of users logged into the proxy.
 * Maps a user's UUID to their {@link dev.emortal.api.model.party.Party} and a party's ObjectID to the party.
 */
final class PartyCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyCache.class);

    private final PartyService partyService;

    private final Map<String, CachedParty> parties = new ConcurrentHashMap<>();

    PartyCache(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    @Nullable CachedParty getParty(@NotNull String partyId) {
        return this.parties.get(partyId);
    }

    @NotNull CachedParty cacheParty(@NotNull Party party) {
        CachedParty cachedParty = CachedParty.fromProto(party);
        this.parties.put(party.getId(), cachedParty);
        return cachedParty;
    }

    @Nullable CachedParty removeParty(@NotNull String partyId) {
        return this.parties.remove(partyId);
    }

    @Blocking
    @Nullable CachedParty cachePartyIfNotPresent(@NotNull String partyId) {
        CachedParty cachedParty = this.getParty(partyId);
        if (cachedParty != null) return cachedParty;

        Party party;
        try {
            party = this.partyService.getParty(partyId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get party", exception);
            return null;
        }

        if (party == null) {
            LOGGER.error("Party with id {} was not found", partyId);
            return null;
        }

        return this.cacheParty(party);
    }

    static final class CachedParty implements LocalParty {

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

        CachedParty(@NotNull String id, @NotNull UUID leaderId, @NotNull Map<UUID, CachedPartyMember> members, boolean open) {
            this.id = id;
            this.members = members;
            this.leaderId = leaderId;
            this.open = open;
        }

        @Override
        public @NotNull String id() {
            return this.id;
        }

        @Override
        public @NotNull UUID leaderId() {
            return this.leaderId;
        }

        @Override
        public boolean open() {
            return this.open;
        }

        @Override
        public int size() {
            return this.members.size();
        }

        @Override
        public @NotNull Collection<UUID> memberIds() {
            return this.members.keySet();
        }

        @Override
        public @NotNull Collection<? extends LocalPartyMember> members() {
            return this.members.values();
        }

        @Override
        public @Nullable LocalPartyMember getMember(@NotNull UUID memberId) {
            return this.members.get(memberId);
        }

        void addMember(@NotNull UUID id, @NotNull PartyMember member) {
            this.members.put(id, CachedPartyMember.fromProto(member));
        }

        void removeMember(@NotNull UUID id) {
            this.members.remove(id);
        }

        void setLeaderId(@NotNull UUID leaderId) {
            this.leaderId = leaderId;
        }

        void setOpen(boolean open) {
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

    private record CachedPartyMember(@NotNull UUID id, @NotNull String username) implements LocalPartyMember {

        static @NotNull CachedPartyMember fromProto(@NotNull PartyMember member) {
            return new CachedPartyMember(UUID.fromString(member.getId()), member.getUsername());
        }
    }
}
