package dev.emortal.testing.service;

import dev.emortal.api.model.common.Pageable;
import dev.emortal.api.model.common.PlayerSkin;
import dev.emortal.api.model.party.EventData;
import dev.emortal.api.model.party.Party;
import dev.emortal.api.model.party.PartyInvite;
import dev.emortal.api.service.party.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DummyPartyService implements PartyService {

    @Override
    public @Nullable Party getParty(@NotNull String partyId) {
        return null;
    }

    @Override
    public @Nullable Party getPartyByPlayer(@NotNull UUID playerId) {
        return null;
    }

    @Override
    public @NotNull ModifyPartyResult emptyParty(@NotNull String s) {
        return ModifyPartyResult.SUCCESS;
    }

    @Override
    public @NotNull ModifyPartyResult emptyPartyByPlayer(@NotNull UUID uuid) {
        return ModifyPartyResult.SUCCESS;
    }

    @Override
    public @NotNull ModifyPartyResult setPartyOpen(@NotNull UUID uuid, boolean b) {
        return ModifyPartyResult.SUCCESS;
    }

    @Override
    public @NotNull List<PartyInvite> getInvites(@NotNull String s, @NotNull Pageable pageable) {
        return List.of();
    }

    @Override
    public @NotNull List<PartyInvite> getInvitesByPlayer(@NotNull UUID uuid, @NotNull Pageable pageable) {
        return List.of();
    }

    @Override
    public @NotNull InvitePlayerToPartyResult invitePlayer(@NotNull UUID uuid, @NotNull String s, @NotNull UUID uuid1, @NotNull String s1) {
        return new InvitePlayerToPartyResult.Success(PartyInvite.getDefaultInstance());
    }

    @Override
    public @NotNull JoinPartyResult joinParty(@NotNull UUID uuid, @NotNull String s, @NotNull UUID uuid1) {
        return new JoinPartyResult.Success(Party.getDefaultInstance());
    }

    @Override
    public @NotNull LeavePartyResult leaveParty(@NotNull UUID uuid) {
        return LeavePartyResult.SUCCESS;
    }

    @Override
    public @NotNull KickPlayerFromPartyResult kickPlayer(@NotNull UUID uuid, @NotNull String s, @NotNull UUID uuid1) {
        return KickPlayerFromPartyResult.SUCCESS;
    }

    @Override
    public @NotNull SetPartyLeaderResult setPartyLeader(@NotNull UUID uuid, @NotNull String s, @NotNull UUID uuid1) {
        return SetPartyLeaderResult.SUCCESS;
    }

    @Override
    public @NotNull EventData createEvent(@NotNull String s, @NotNull UUID uuid, @NotNull String s1, @NotNull PlayerSkin playerSkin, @Nullable Instant instant, @Nullable Instant instant1) {
        return null;
    }

    @Override
    public @NotNull EventData updateEvent(@NotNull String s, @Nullable Instant instant, @Nullable Instant instant1) {
        return null;
    }

    @Override
    public @NotNull List<EventData> listEvents() {
        return List.of();
    }

    @Override
    public DeleteEventResult deleteEvent(@Nullable String s) {
        return null;
    }
}
