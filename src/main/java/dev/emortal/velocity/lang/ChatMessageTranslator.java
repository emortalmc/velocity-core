package dev.emortal.velocity.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ChatMessageTranslator {
    private static final Pattern ARG_PATTERN = Pattern.compile("\\{\\d+}");

    static @NotNull Component translate(@NotNull Component input, @NotNull List<Component> args) {
        Component output = input;
        if (!args.isEmpty()) {
            output = output.replaceText(TextReplacementConfig.builder()
                    .match(ARG_PATTERN)
                    .replacement((match, builder) -> doReplacement(match, args))
                    .build());
            output = translateClickEvent(output, args);
            output = translateHoverEvent(output, args);
        }
        return translateChildren(output, args);
    }

    private static @NotNull Component translateChildren(@NotNull Component input, @NotNull List<Component> args) {
        List<Component> children = new ArrayList<>();
        for (Component component : input.children()) {
            Component output = translate(component, args);
            children.add(translateChildren(output, args));
        }
        return input.children(children);
    }

    private static @NotNull Component translateClickEvent(@NotNull Component input, @NotNull List<Component> args) {
        ClickEvent event = input.clickEvent();
        if (event == null) return input;

        String value = event.value();
        Matcher matcher = ARG_PATTERN.matcher(value);

        String result = matcher.replaceAll(match -> PlainTextComponentSerializer.plainText().serialize(doReplacement(match, args)));
        return input.clickEvent(ClickEvent.clickEvent(event.action(), result));
    }

    private static @NotNull Component translateHoverEvent(@NotNull Component input, @NotNull List<Component> args) {
        HoverEvent<?> event = input.hoverEvent();
        if (event == null) return input;
        if (event.action() != HoverEvent.Action.SHOW_TEXT) return input;

        @SuppressWarnings("unchecked") // we check the type above
        HoverEvent.Action<Component> action = (HoverEvent.Action<Component>) event.action();

        Component value = (Component) event.value();
        Component result = value.replaceText(TextReplacementConfig.builder()
                .match(ARG_PATTERN)
                .replacement((match, builder) -> doReplacement(match, args))
                .build());

        return input.hoverEvent(HoverEvent.hoverEvent(action, result));
    }

    private static @NotNull Component doReplacement(@NotNull MatchResult match, @NotNull List<Component> args) {
        String group = match.group();
        int index = Integer.parseInt(group.substring(1, group.length() - 1));
        return index < args.size() ? args.get(index) : Component.text("$$" + index);
    }

    private ChatMessageTranslator() {
    }
}
