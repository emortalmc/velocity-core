package dev.emortal.velocity.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

final class MessageColors {

    static final @NotNull TextColor PARTY_LIST_BLUE = TextColor.color(0x2383d1);
    static final @NotNull TextColor PARTY_LIST_HEADER_BLUE = TextColor.color(0x2ba0ff);
    static final @NotNull TextColor PURPLE_NAME = TextColor.color(0xc98fff);
    static final @NotNull TextColor PRIVATE_MESSAGE_NAME = TextColor.color(0xff9ef5);
    static final @NotNull TextColor TAB_LIST_FOOTER_IP = TextColor.color(0x266ee0);

    static @NotNull Component purpleName(@NotNull String text) {
        return Component.text(text, PURPLE_NAME);
    }

    private MessageColors() {
    }
}
