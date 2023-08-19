package dev.emortal.velocity.party;

import dev.emortal.api.model.party.Party;
import dev.emortal.testing.RandomIdGenerator;
import dev.emortal.testing.service.DummyPartyService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class PartyCacheTest {

    @Test
    void testCachePartyIfNotPresentReturnsNullIfError() {
        class TestPartyService extends DummyPartyService {
            @Override
            public @Nullable Party getParty(@NotNull String partyId) {
                throw new StatusRuntimeException(Status.UNKNOWN);
            }
        }

        PartyCache cache = new PartyCache(new TestPartyService());
        PartyCache.CachedParty party = cache.cachePartyIfNotPresent("test");
        assertNull(party);
    }

    @Test
    void testCachePartyIfNotPresentReturnsNullIfPartyNull() {
        class TestPartyService extends DummyPartyService {
            @Override
            public @Nullable Party getParty(@NotNull String partyId) {
                return null;
            }
        }

        PartyCache cache = new PartyCache(new TestPartyService());
        PartyCache.CachedParty party = cache.cachePartyIfNotPresent("test");
        assertNull(party);
    }

    @Test
    void testCachePartyIfNotPresentReturnsPartyWhenPresent() {
        String partyId = RandomIdGenerator.randomId();
        UUID leaderId = RandomIdGenerator.randomUUID();
        Party party = Party.newBuilder().setId(partyId).setLeaderId(leaderId.toString()).build();

        class TestPartyService extends DummyPartyService {
            @Override
            public @Nullable Party getParty(@NotNull String partyId) {
                return party;
            }
        }

        PartyCache cache = new PartyCache(new TestPartyService());
        PartyCache.CachedParty cachedParty = cache.cachePartyIfNotPresent(partyId);

        assertNotNull(cachedParty);
        assertEquals(partyId, cachedParty.id());
        assertEquals(leaderId, cachedParty.leaderId());
    }
}
