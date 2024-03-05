package dev.emortal.velocity.party.cache;

import dev.emortal.api.model.party.Party;
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

    default @NotNull Party toProto() {
        Party.Builder builder = Party.newBuilder()
                .setId(this.id())
                .setLeaderId(this.leaderId().toString())
                .setOpen(this.open());

        for (LocalPartyMember member : this.members()) {
            builder.addMembers(member.toProto());
        }

        return builder.build();
    }
}
