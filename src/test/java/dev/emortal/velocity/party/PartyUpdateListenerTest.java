package dev.emortal.velocity.party;

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
import dev.emortal.testing.DummyMessageHandler;
import dev.emortal.testing.party.DummyPartyUpdateNotifier;
import dev.emortal.testing.DummyPlayer;
import dev.emortal.testing.FixedPlayerProvider;
import dev.emortal.testing.RandomIdGenerator;
import dev.emortal.testing.service.DummyPartyService;
import dev.emortal.velocity.party.cache.LocalParty;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public final class PartyUpdateListenerTest {

    @Test
    void testPartyCreationWithPlayerNotOnline() {
        String partyId = RandomIdGenerator.randomId();
        UUID leaderId = RandomIdGenerator.randomUUID();
        Party party = Party.newBuilder().setId(partyId).setLeaderId(leaderId.toString()).build();

        PartyCache cache = new PartyCache(new DummyPartyService());
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), new DummyPartyUpdateNotifier(), new DummyMessageHandler());
        listener.handleCreateParty(PartyCreatedMessage.newBuilder().setParty(party).build());

        assertNull(cache.getParty(partyId));
    }

    @Test
    void testPartyCreationWithPlayerOnline() {
        String partyId = RandomIdGenerator.randomId();
        UUID playerId = RandomIdGenerator.randomUUID();
        Party party = Party.newBuilder().setId(partyId).setLeaderId(playerId.toString()).build();

        class TestPlayer extends DummyPlayer {
            @Override
            public UUID getUniqueId() {
                return playerId;
            }
        }

        PartyCache cache = new PartyCache(new DummyPartyService());
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(new TestPlayer()), new DummyPartyUpdateNotifier(), new DummyMessageHandler());
        listener.handleCreateParty(PartyCreatedMessage.newBuilder().setParty(party).build());

        assertNotNull(cache.getParty(partyId));
    }

    @Test
    void testPartyDoesNotEmptyIfNotCached() {
        class FailNotifier extends DummyPartyUpdateNotifier {
            @Override
            public void partyEmptied(@NotNull UUID memberId) {
                fail("Party should not be emptied if not cached");
            }
        }

        PartyCache cache = new PartyCache(new DummyPartyService());
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), new FailNotifier(), new DummyMessageHandler());

        Party party = Party.newBuilder()
                .setId(RandomIdGenerator.randomId())
                .addMembers(PartyMember.newBuilder().setId(RandomIdGenerator.randomId()).build())
                .build();
        listener.handlePartyEmptied(PartyEmptiedMessage.newBuilder().setParty(party).build());
    }

    @Test
    void testPartyDoesNotRemoveLeaderWhenEmptied() {
        UUID leaderId = RandomIdGenerator.randomUUID();
        Party party = Party.newBuilder()
                .setId(RandomIdGenerator.randomId())
                .setLeaderId(leaderId.toString())
                .addMembers(PartyMember.newBuilder().setId(leaderId.toString()).build()) // leader
                .addMembers(PartyMember.newBuilder().setId(RandomIdGenerator.randomId()).build())
                .build();

        class FailNotifier extends DummyPartyUpdateNotifier {
            @Override
            public void partyEmptied(@NotNull UUID memberId) {
                if (memberId.equals(leaderId)) fail("Leader should not be removed when party is emptied");
            }
        }

        PartyCache cache = new PartyCache(new DummyPartyService());
        cache.cacheParty(party);

        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), new FailNotifier(), new DummyMessageHandler());
        listener.handlePartyEmptied(PartyEmptiedMessage.newBuilder().setParty(party).build());
    }

    @Test
    void testMemberRemovedWhenPartyEmptied() {
        UUID leaderId = RandomIdGenerator.randomUUID();
        UUID memberId = RandomIdGenerator.randomUUID();
        Party party = Party.newBuilder()
                .setId(RandomIdGenerator.randomId())
                .setLeaderId(leaderId.toString())
                .addMembers(PartyMember.newBuilder().setId(leaderId.toString()).build()) // leader
                .addMembers(PartyMember.newBuilder().setId(memberId.toString()).build()) // member
                .build();

        class PlayerRemovedNotifier extends DummyPartyUpdateNotifier {

            boolean memberRemoved = false;

            @Override
            public void partyEmptied(@NotNull UUID removedMemberId) {
                if (!removedMemberId.equals(memberId)) fail("Only expected member should be removed");
                this.memberRemoved = true;
            }
        }

        PartyCache cache = new PartyCache(new DummyPartyService());
        LocalParty cachedParty = cache.cacheParty(party);

        PlayerRemovedNotifier notifier = new PlayerRemovedNotifier();
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), notifier, new DummyMessageHandler());
        listener.handlePartyEmptied(PartyEmptiedMessage.newBuilder().setParty(party).build());

        assertTrue(notifier.memberRemoved);
        assertNull(cachedParty.getMember(memberId));
    }

    @Test
    void testPartyClosedWhenEmptied() {
        class PartyClosedNotifier extends DummyPartyUpdateNotifier {

            boolean partyClosed = false;

            @Override
            public void partyOpenStateChanged(@NotNull LocalParty party, boolean open) {
                this.partyClosed = true;
                assertFalse(open);
            }
        }

        Party party = Party.newBuilder().setId(RandomIdGenerator.randomId()).setLeaderId(RandomIdGenerator.randomId()).build();
        PartyCache cache = new PartyCache(new DummyPartyService());
        cache.cacheParty(party);

        PartyClosedNotifier notifier = new PartyClosedNotifier();
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), notifier, new DummyMessageHandler());
        listener.handlePartyEmptied(PartyEmptiedMessage.newBuilder().setParty(party).build());

        assertTrue(notifier.partyClosed);
    }

    @Test
    void testPartyNotOpenedIfNotCached() {
        class FailNotifier extends DummyPartyUpdateNotifier {
            @Override
            public void partyOpenStateChanged(@NotNull LocalParty party, boolean open) {
                fail("Party should not have open state changed if not cached");
            }
        }

        PartyCache cache = new PartyCache(new DummyPartyService());
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), new FailNotifier(), new DummyMessageHandler());
        listener.handlePartyOpenChanged(PartyOpenChangedMessage.newBuilder().setPartyId(RandomIdGenerator.randomId()).setOpen(true).build());
    }

    @Test
    void testPartyOpenedWhenRequested() {
        class PartyOpenedNotifier extends DummyPartyUpdateNotifier {

            boolean partyOpened = false;

            @Override
            public void partyOpenStateChanged(@NotNull LocalParty party, boolean open) {
                this.partyOpened = true;
                assertTrue(open);
            }
        }

        Party party = Party.newBuilder().setId(RandomIdGenerator.randomId()).setLeaderId(RandomIdGenerator.randomId()).setOpen(false).build();
        PartyCache cache = new PartyCache(new DummyPartyService());
        cache.cacheParty(party);

        PartyOpenedNotifier notifier = new PartyOpenedNotifier();
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), notifier, new DummyMessageHandler());
        listener.handlePartyOpenChanged(PartyOpenChangedMessage.newBuilder().setPartyId(party.getId()).setOpen(true).build());

        assertTrue(notifier.partyOpened);
    }

    @Test
    void testPartyClosedWhenRequested() {
        class PartyClosedNotifier extends DummyPartyUpdateNotifier {

            boolean partyClosed = false;

            @Override
            public void partyOpenStateChanged(@NotNull LocalParty party, boolean open) {
                this.partyClosed = true;
                assertFalse(open);
            }
        }

        Party party = Party.newBuilder().setId(RandomIdGenerator.randomId()).setLeaderId(RandomIdGenerator.randomId()).setOpen(true).build();
        PartyCache cache = new PartyCache(new DummyPartyService());
        cache.cacheParty(party);

        PartyClosedNotifier notifier = new PartyClosedNotifier();
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), notifier, new DummyMessageHandler());
        listener.handlePartyOpenChanged(PartyOpenChangedMessage.newBuilder().setPartyId(party.getId()).setOpen(false).build());

        assertTrue(notifier.partyClosed);
    }

    @Test
    void testMemberNotifiedWhenPartyDeleted() {
        UUID leaderId = RandomIdGenerator.randomUUID();
        UUID memberId = RandomIdGenerator.randomUUID();
        Party party = Party.newBuilder()
                .setId(RandomIdGenerator.randomId())
                .setLeaderId(leaderId.toString())
                .addMembers(PartyMember.newBuilder().setId(leaderId.toString()).build()) // leader
                .addMembers(PartyMember.newBuilder().setId(memberId.toString()).build()) // member
                .build();

        class MemberRemovedNotifier extends DummyPartyUpdateNotifier {

            boolean memberRemoved = false;

            @Override
            public void partyDeleted(@NotNull UUID removedMemberId) {
                this.memberRemoved = true;
            }
        }

        PartyCache cache = new PartyCache(new DummyPartyService());
        cache.cacheParty(party);

        MemberRemovedNotifier notifier = new MemberRemovedNotifier();
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), notifier, new DummyMessageHandler());
        listener.handlePartyDeleted(PartyDeletedMessage.newBuilder().setParty(party).build());

        assertTrue(notifier.memberRemoved);
    }

    @Test
    void testMemberNotNotifiedWhenPartyDeletedIfNotCached() {
        class FailNotifier extends DummyPartyUpdateNotifier {
            @Override
            public void partyDeleted(@NotNull UUID memberId) {
                fail("Party should not be deleted if not cached");
            }
        }

        Party party = Party.newBuilder()
                .setId(RandomIdGenerator.randomId())
                .setLeaderId(RandomIdGenerator.randomId())
                .addMembers(PartyMember.newBuilder().setId(RandomIdGenerator.randomId()).build())
                .build();
        PartyCache cache = new PartyCache(new DummyPartyService());

        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), new FailNotifier(), new DummyMessageHandler());
        listener.handlePartyDeleted(PartyDeletedMessage.newBuilder().setParty(party).build());
    }

    @Test
    void testMemberNotNotifiedWhenPartyDeletedIfOnlyLeader() {
        class FailNotifier extends DummyPartyUpdateNotifier {
            @Override
            public void partyDeleted(@NotNull UUID memberId) {
                fail("Party should not be deleted if it is only one member");
            }
        }

        Party party = Party.newBuilder()
                .setId(RandomIdGenerator.randomId())
                .setLeaderId(RandomIdGenerator.randomId())
                .addMembers(PartyMember.newBuilder().setId(RandomIdGenerator.randomId()).build())
                .build();
        PartyCache cache = new PartyCache(new DummyPartyService());
        cache.cacheParty(party);

        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), new FailNotifier(), new DummyMessageHandler());
        listener.handlePartyDeleted(PartyDeletedMessage.newBuilder().setParty(party).build());
    }

    @Test
    void testPlayerNotJoinedIfNotOnline() {
        class FailNotifier extends DummyPartyUpdateNotifier {
            @Override
            public void playerJoined(@NotNull LocalParty party, @NotNull String joinerName) {
                fail("Player should not be joined if not online");
            }
        }

        String partyId = RandomIdGenerator.randomId();
        UUID memberId = RandomIdGenerator.randomUUID();

        PartyCache cache = new PartyCache(new DummyPartyService());
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), new FailNotifier(), new DummyMessageHandler());

        PartyPlayerJoinedMessage message = PartyPlayerJoinedMessage.newBuilder()
                .setPartyId(partyId)
                .setMember(PartyMember.newBuilder().setId(memberId.toString()).build())
                .build();
        listener.handleJoinParty(message);
    }

    @Test
    void testPlayerNotJoinedIfPartyNotCached() {
        class FailNotifier extends DummyPartyUpdateNotifier {
            @Override
            public void playerJoined(@NotNull LocalParty party, @NotNull String joinerName) {
                fail("Player should not be joined if party not cached");
            }
        }

        String partyId = RandomIdGenerator.randomId();
        UUID memberId = RandomIdGenerator.randomUUID();

        class TestPlayer extends DummyPlayer {
            @Override
            public UUID getUniqueId() {
                return memberId;
            }
        }

        PartyCache cache = new PartyCache(new DummyPartyService());
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(new TestPlayer()), new FailNotifier(), new DummyMessageHandler());

        PartyPlayerJoinedMessage message = PartyPlayerJoinedMessage.newBuilder()
                .setPartyId(partyId)
                .setMember(PartyMember.newBuilder().setId(memberId.toString()).build())
                .build();
        listener.handleJoinParty(message);
    }

    @Test
    void testPlayerJoined() {
        String partyId = RandomIdGenerator.randomId();
        UUID memberId = RandomIdGenerator.randomUUID();

        class PlayerJoinedNotifier extends DummyPartyUpdateNotifier {

            boolean playerJoined = false;

            @Override
            public void playerJoined(@NotNull LocalParty party, @NotNull String joinerName) {
                this.playerJoined = true;
                assertEquals(party.id(), partyId);
                assertEquals(joinerName, "TestPlayer");
            }
        }

        class TestPlayer extends DummyPlayer {
            @Override
            public UUID getUniqueId() {
                return memberId;
            }

            @Override
            public String getUsername() {
                return "TestPlayer";
            }
        }

        Party party = Party.newBuilder().setId(partyId).setLeaderId(RandomIdGenerator.randomId()).build();
        PartyCache cache = new PartyCache(new DummyPartyService());
        cache.cacheParty(party);

        PlayerJoinedNotifier notifier = new PlayerJoinedNotifier();
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(new TestPlayer()), notifier, new DummyMessageHandler());

        PartyPlayerJoinedMessage message = PartyPlayerJoinedMessage.newBuilder()
                .setPartyId(partyId)
                .setMember(PartyMember.newBuilder().setId(memberId.toString()).build())
                .build();
        listener.handleJoinParty(message);
    }

    @Test
    void testNoKickNotificationWhenLeft() {
        class FailNotifier extends DummyPartyUpdateNotifier {
            @Override
            public void selfKicked(@NotNull UUID targetId, @NotNull String kickerName) {
                fail("Player should not be kicked if left");
            }

            @Override
            public void playerKicked(@NotNull LocalParty party, @NotNull String targetName, @NotNull String kickerName) {
                fail("Player should not be kicked if left");
            }
        }

        String partyId = RandomIdGenerator.randomId();
        UUID memberId = RandomIdGenerator.randomUUID();
        Party party = Party.newBuilder().setId(partyId).setLeaderId(memberId.toString()).build();

        PartyCache cache = new PartyCache(new DummyPartyService());
        cache.cacheParty(party);

        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), new FailNotifier(), new DummyMessageHandler());
        PartyMember member = PartyMember.newBuilder().setId(memberId.toString()).setUsername("TestPlayer").build();
        listener.handleLeaveParty(PartyPlayerLeftMessage.newBuilder().setPartyId(partyId).setMember(member).build());
    }

    @Test
    void testSelfNotifiedWhenKickedIfNotCached() {
        UUID targetId = RandomIdGenerator.randomUUID();
        class SelfKickedNotifier extends DummyPartyUpdateNotifier {

            boolean selfKicked = false;

            @Override
            public void selfKicked(@NotNull UUID kickTargetId, @NotNull String kickerName) {
                this.selfKicked = true;
                assertEquals(targetId, kickTargetId);
            }

            @Override
            public void playerKicked(@NotNull LocalParty party, @NotNull String targetName, @NotNull String kickerName) {
                fail("Other members should not be notified of kick if party not cached");
            }
        }

        String partyId = RandomIdGenerator.randomId();
        PartyCache cache = new PartyCache(new DummyPartyService());

        SelfKickedNotifier notifier = new SelfKickedNotifier();
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), notifier, new DummyMessageHandler());

        PartyPlayerLeftMessage message = PartyPlayerLeftMessage.newBuilder()
                .setPartyId(partyId)
                .setMember(PartyMember.newBuilder().setId(targetId.toString()).setUsername("TestPlayer").build())
                .setKickedBy(PartyMember.newBuilder().setId(RandomIdGenerator.randomId()).setUsername("TestKicker").build())
                .build();
        listener.handleLeaveParty(message);

        assertTrue(notifier.selfKicked);
    }

    @Test
    void testSelfAndMemberNotifiedWhenKicked() {
        UUID targetId = RandomIdGenerator.randomUUID();
        class SelfAndMemberKickedNotifier extends DummyPartyUpdateNotifier {

            boolean selfKicked = false;
            boolean memberKicked = false;

            @Override
            public void selfKicked(@NotNull UUID kickTargetId, @NotNull String kickerName) {
                this.selfKicked = true;
                assertEquals(targetId, kickTargetId);
                assertEquals("TestKicker", kickerName);
            }

            @Override
            public void playerKicked(@NotNull LocalParty party, @NotNull String targetName, @NotNull String kickerName) {
                this.memberKicked = true;
                assertEquals("TestPlayer", targetName);
                assertEquals("TestKicker", kickerName);
            }
        }

        String partyId = RandomIdGenerator.randomId();
        Party party = Party.newBuilder()
                .setId(partyId)
                .setLeaderId(RandomIdGenerator.randomId())
                .addMembers(PartyMember.newBuilder().setId(RandomIdGenerator.randomId()).setUsername("TestMember").build())
                .build();

        PartyCache cache = new PartyCache(new DummyPartyService());
        cache.cacheParty(party);

        SelfAndMemberKickedNotifier notifier = new SelfAndMemberKickedNotifier();
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), notifier, new DummyMessageHandler());

        PartyPlayerLeftMessage message = PartyPlayerLeftMessage.newBuilder()
                .setPartyId(partyId)
                .setMember(PartyMember.newBuilder().setId(targetId.toString()).setUsername("TestPlayer").build())
                .setKickedBy(PartyMember.newBuilder().setId(RandomIdGenerator.randomId()).setUsername("TestKicker").build())
                .build();
        listener.handleLeaveParty(message);

        assertTrue(notifier.selfKicked);
        assertTrue(notifier.memberKicked);
    }

    @Test
    void testLeaderNotChangedIfNotCached() {
        class FailNotifier extends DummyPartyUpdateNotifier {
            @Override
            public void partyLeaderChanged(@NotNull LocalParty party, @NotNull String newLeaderName) {
                fail("Leader should not be changed if party is not cached");
            }
        }

        String partyId = RandomIdGenerator.randomId();
        UUID leaderId = RandomIdGenerator.randomUUID();
        PartyMember newLeader = PartyMember.newBuilder().setId(leaderId.toString()).build();

        PartyCache cache = new PartyCache(new DummyPartyService());
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), new FailNotifier(), new DummyMessageHandler());
        listener.handleLeaderChange(PartyLeaderChangedMessage.newBuilder().setPartyId(partyId).setNewLeader(newLeader).build());
    }

    @Test
    void testLeaderChangedWhenCached() {
        String partyId = RandomIdGenerator.randomId();
        UUID leaderId = RandomIdGenerator.randomUUID();
        String leaderName = "TestNewLeader";

        class LeaderChangedNotifier extends DummyPartyUpdateNotifier {

            boolean leaderChanged = false;

            @Override
            public void partyLeaderChanged(@NotNull LocalParty party, @NotNull String newLeaderName) {
                this.leaderChanged = true;
                assertEquals(partyId, party.id());
                assertEquals(leaderName, newLeaderName);
            }
        }

        Party party = Party.newBuilder().setId(partyId).setLeaderId(leaderId.toString()).build();
        PartyMember newLeader = PartyMember.newBuilder().setId(leaderId.toString()).setUsername(leaderName).build();

        PartyCache cache = new PartyCache(new DummyPartyService());
        cache.cacheParty(party);

        LeaderChangedNotifier notifier = new LeaderChangedNotifier();
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), notifier, new DummyMessageHandler());
        listener.handleLeaderChange(PartyLeaderChangedMessage.newBuilder().setPartyId(partyId).setNewLeader(newLeader).build());

        assertTrue(notifier.leaderChanged);
    }

    @Test
    void testSelfInvitedIfNotCached() {
        String partyId = RandomIdGenerator.randomId();
        UUID targetId = RandomIdGenerator.randomUUID();
        String targetName = "TestSender";

        class SelfInvitedNotifier extends DummyPartyUpdateNotifier {

            boolean selfInvited = false;

            @Override
            public void selfInvited(@NotNull UUID invitedTargetId, @NotNull String senderName) {
                this.selfInvited = true;
                assertEquals(targetId, invitedTargetId);
                assertEquals(targetName, senderName);
            }
        }

        PartyInvite invite = PartyInvite.newBuilder().setPartyId(partyId).setTargetId(targetId.toString()).setSenderUsername(targetName).build();
        PartyCache cache = new PartyCache(new DummyPartyService());

        SelfInvitedNotifier notifier = new SelfInvitedNotifier();
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), notifier, new DummyMessageHandler());
        listener.handleInviteCreated(PartyInviteCreatedMessage.newBuilder().setInvite(invite).build());

        assertTrue(notifier.selfInvited);
    }

    @Test
    void testMemberInvitedWhenCached() {
        String partyId = RandomIdGenerator.randomId();
        UUID senderId = RandomIdGenerator.randomUUID();
        String senderName = "TestSender";
        UUID targetId = RandomIdGenerator.randomUUID();
        String targetName = "TestPlayer";

        class MemberInvitedNotifier extends DummyPartyUpdateNotifier {

            boolean selfInvited = false;
            boolean inviteCreated = false;

            @Override
            public void selfInvited(@NotNull UUID invitedId, @NotNull String inviterName) {
                this.selfInvited = true;
                assertEquals(targetId, invitedId);
                assertEquals(senderName, inviterName);
            }

            @Override
            public void partyInviteCreated(@NotNull LocalParty party, @NotNull UUID inviterId, @NotNull String inviterName,
                                           @NotNull String invitedName) {
                this.inviteCreated = true;
                assertEquals(partyId, party.id());
                assertEquals(senderId, inviterId);
                assertEquals(senderName, inviterName);
                assertEquals(targetName, invitedName);
            }
        }

        Party party = Party.newBuilder().setId(partyId).setLeaderId(senderId.toString()).build();
        PartyInvite invite = PartyInvite.newBuilder()
                .setPartyId(partyId)
                .setSenderId(senderId.toString())
                .setSenderUsername(senderName)
                .setTargetId(targetId.toString())
                .setTargetUsername(targetName)
                .build();

        PartyCache cache = new PartyCache(new DummyPartyService());
        cache.cacheParty(party);

        MemberInvitedNotifier notifier = new MemberInvitedNotifier();
        PartyUpdateListener listener = new PartyUpdateListener(cache, new FixedPlayerProvider(null), notifier, new DummyMessageHandler());
        listener.handleInviteCreated(PartyInviteCreatedMessage.newBuilder().setInvite(invite).build());

        assertTrue(notifier.selfInvited);
        assertTrue(notifier.inviteCreated);
    }
}
