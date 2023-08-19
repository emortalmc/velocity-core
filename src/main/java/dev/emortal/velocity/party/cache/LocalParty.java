package dev.emortal.velocity.party.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public interface LocalParty {

    @NotNull String id();

    @NotNull UUID leaderId();

    boolean open();

    int size();

    @NotNull Collection<UUID> memberIds();

    @NotNull Collection<? extends LocalPartyMember> members();

    @Nullable LocalPartyMember getMember(@NotNull UUID id);
}
