package dev.emortal.velocity.lang;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum TempLang {
    PLAYER_NOT_FOUND("Could not find player <search_username>");

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final @NotNull String unparsed;
    private final @NotNull Set<String> tags;

    TempLang(@NotNull String unparsed) {
        this.unparsed = unparsed;

        this.tags = Pattern.compile("<(.*?)>").matcher(unparsed)
                .results()
                .map(result -> result.group(1))
                .collect(Collectors.toUnmodifiableSet());
    }

    public @NotNull Component deserialize(TagResolver... tagResolvers) {
        TagResolver resolver = TagResolver.resolver(tagResolvers);
        for (String tag : this.tags) {
            if (!resolver.has(tag)) {
                throw new IllegalArgumentException("Tag %s is not resolved".formatted(tag));
            }
        }

        return MINI_MESSAGE.deserialize(this.unparsed, tagResolvers);
    }

    public void send(@NotNull Audience target, TagResolver... tagResolvers) {
        target.sendMessage(this.deserialize(tagResolvers));
    }

    public @NotNull Set<String> getTags() {
        return this.tags;
    }
}
