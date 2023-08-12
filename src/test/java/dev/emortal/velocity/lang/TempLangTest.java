package dev.emortal.velocity.lang;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class TempLangTest {

    /**
     * This isn't meant to be a serious test, just a bunch of conditions shammed into one.
     */
    @Test
    public void genericTest() {
        Assertions.assertThrows(IllegalArgumentException.class, TempLang.PLAYER_NOT_FOUND::deserialize);
        Assertions.assertDoesNotThrow(() -> TempLang.PLAYER_NOT_FOUND.deserialize(Placeholder.unparsed("search_username", "test")));

        Assertions.assertEquals(TempLang.PLAYER_NOT_FOUND.tags().size(), 1);
        Assertions.assertEquals(TempLang.PLAYER_NOT_FOUND.tags().iterator().next(), "search_username");
    }
}
