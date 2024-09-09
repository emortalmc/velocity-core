package dev.emortal.velocity.misc.listener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.CookieReceiveEvent;
import dev.emortal.velocity.lang.ChatMessages;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LoginCookieVerification {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginCookieVerification.class);

    private static final Key COOKIE_NAME = net.kyori.adventure.key.Key.key("emortalmc", "proxy_route_token");

    private static final SecretKey SIGNING_KEY;

    static {
        String signingKey = System.getenv("EDGE_ROUTING_KEY");
        SIGNING_KEY = Keys.hmacShaKeyFor(signingKey.getBytes());
    }

    private final Cache<UUID, Consumer<@Nullable CookieResult>> pendingPlayers = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .evictionListener(this::onEvict)
            .build();

    @Subscribe
    public void onLogin(@NotNull LoginEvent event, Continuation continuation) {
        event.getPlayer().requestCookie(COOKIE_NAME);

        this.pendingPlayers.put(event.getPlayer().getUniqueId(), result -> {
            if (result == null) {
                event.getPlayer().disconnect(ChatMessages.ERROR_INVALID_TRANSFER_COOKIE.get(CookieResult.INVALID_JWT.name()));
            } else if (!result.isValid()) {
                event.getPlayer().disconnect(ChatMessages.ERROR_INVALID_TRANSFER_COOKIE.get(result.name()));
            }

            continuation.resume();
        });
    }

    @Subscribe
    public void onCookieReceived(@NotNull CookieReceiveEvent event) {
        System.out.println("Received cookie event " + event.getOriginalKey());
        if (!event.getOriginalKey().equals(COOKIE_NAME)) return;

        Consumer<CookieResult> consumer = this.pendingPlayers.getIfPresent(event.getPlayer().getUniqueId());
        if (consumer == null) {
            LOGGER.warn("Received cookie for player '{}' without pending request", event.getPlayer().getUsername());
            return;
        }

        event.setResult(CookieReceiveEvent.ForwardResult.handled());
        CookieResult result = this.isCookieValid(event.getPlayer().getUsername(), event.getOriginalData());
        consumer.accept(result);
    }

    private CookieResult isCookieValid(String username, byte[] value) {
        if (value == null || value.length == 0) return CookieResult.EMPTY_COOKIE;

        String jwt = new String(value, StandardCharsets.UTF_8);

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(SIGNING_KEY).build()
                    .parseSignedClaims(jwt)
                    .getPayload();
        } catch (JwtException e) {
            LOGGER.warn("Rejected JWT cookie for '{}' '{}'", username, jwt, e);
            return CookieResult.INVALID_JWT;
        }

        if (System.currentTimeMillis() > claims.getExpiration().getTime()) return CookieResult.EXPIRED;
        if (!claims.get("proxyId", String.class).equals(System.getenv("HOSTNAME"))) return CookieResult.WRONG_PROXY_ID;
        if (!claims.get("username", String.class).equals(username)) return CookieResult.WRONG_USERNAME;

        return CookieResult.VALID;
    }

    private void onEvict(@Nullable UUID playerId, @Nullable Consumer<@Nullable CookieResult> consumer, @NotNull RemovalCause cause) {
        if (cause != RemovalCause.EXPIRED) return;
        if (playerId == null || consumer == null) return;

        consumer.accept(null);
    }

    private enum CookieResult {
        EMPTY_COOKIE(false),
        INVALID_JWT(false),
        WRONG_PROXY_ID(false),
        WRONG_USERNAME(false),
        EXPIRED(false),
        VALID(true);

        private final boolean valid;

        CookieResult(boolean valid) {
            this.valid = valid;
        }

        public boolean isValid() {
            return this.valid;
        }
    }
}
