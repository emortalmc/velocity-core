package dev.emortal.velocity.party.cache;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface LocalPartyMember {

    @NotNull UUID id();

    @NotNull String username();
}
