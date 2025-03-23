package dev.emortal.testing.party;

import dev.emortal.api.model.party.Party;
import dev.emortal.velocity.party.cache.LocalParty;
import dev.emortal.velocity.party.notifier.PartyUpdateNotifier;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DummyPartyUpdateNotifier implements PartyUpdateNotifier {

    @Override
    public void partyEmptied(@NotNull UUID memberId) {
    }

    @Override
    public void partyDeleted(@NotNull UUID memberId) {
    }

    @Override
    public void partyOpenStateChanged(@NotNull LocalParty party, boolean open) {
    }

    @Override
    public void partyLeaderChanged(@NotNull LocalParty party, @NotNull String newLeaderName) {
    }

    @Override
    public void partyInviteCreated(@NotNull LocalParty party, @NotNull UUID senderId, @NotNull String senderName, @NotNull String targetName) {
    }

    @Override
    public void selfInvited(@NotNull UUID targetId, @NotNull String senderName) {
    }

    @Override
    public void playerJoined(@NotNull LocalParty party, @NotNull String joinerName) {
    }

    @Override
    public void playerLeft(@NotNull LocalParty party, @NotNull String leaverName) {
    }

    @Override
    public void playerKicked(@NotNull LocalParty party, @NotNull String targetName, @NotNull String kickerName) {
    }

    @Override
    public void selfKicked(@NotNull UUID targetId, @NotNull String kickerName) {
    }

    @Override
    public void partyBroadcast(@NotNull Party party) {
    }
}
