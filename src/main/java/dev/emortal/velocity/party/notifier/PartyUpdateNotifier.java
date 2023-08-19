package dev.emortal.velocity.party.notifier;

import dev.emortal.velocity.party.cache.LocalParty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface PartyUpdateNotifier {

    void partyEmptied(@NotNull UUID memberId);

    void partyDeleted(@NotNull UUID memberId);

    void partyOpenStateChanged(@NotNull LocalParty party, boolean open);

    void partyLeaderChanged(@NotNull LocalParty party, @NotNull String newLeaderName);

    void partyInviteCreated(@NotNull LocalParty party, @NotNull UUID senderId, @NotNull String senderName, @NotNull String targetName);

    void selfInvited(@NotNull UUID targetId, @NotNull String senderName);

    void playerJoined(@NotNull LocalParty party, @NotNull String joinerName);

    void playerLeft(@NotNull LocalParty party, @NotNull String leaverName);

    void playerKicked(@NotNull LocalParty party, @NotNull String targetName, @NotNull String kickerName);

    void selfKicked(@NotNull UUID targetId, @NotNull String kickerName);
}
