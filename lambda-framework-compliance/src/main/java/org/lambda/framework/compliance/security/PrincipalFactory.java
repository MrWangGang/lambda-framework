package org.lambda.framework.compliance.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.StringCodec;
import org.bson.types.ObjectId;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.po.ScopedLoginUser;
import org.lambda.framework.common.po.SecurityLoginUser;
import org.lambda.framework.common.support.SecurityStash;
import org.lambda.framework.common.util.JsonUtil;
import org.lambda.framework.common.util.IdUtil;
import org.lambda.framework.enums.ComplianceExceptionEnum;
import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.regex.Pattern;

import static org.lambda.framework.common.enums.ConmonContract.LAMBDA_SECURITY_AUTH_TOKEN_KEY;
import static org.lambda.framework.common.enums.ConmonContract.LAMBDA_SECURITY_AUTH_TOKEN_REGEX;
import static org.lambda.framework.common.enums.ConmonContract.LAMBDA_SECURITY_TOKEN_TIME_SECOND;
import static org.lambda.framework.enums.ComplianceExceptionEnum.CONTEXT_INVALID;
import static org.lambda.framework.enums.ComplianceExceptionEnum.SESSION_INVALID;

public final class PrincipalFactory implements SmartInitializingSingleton, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(PrincipalFactory.class);
    private static final int EPOCH_SLOTS = 256;
    private static final long TRACKING_RETRY_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final Pattern TOKEN_FORMAT = Pattern.compile(LAMBDA_SECURITY_AUTH_TOKEN_REGEX);
    private static final Pattern USER_ID_FORMAT = Pattern.compile("[a-zA-Z0-9]+");
    private static final RedisScript<Long> CHANGE_SESSION = RedisScript.of("""
            local stored = redis.call('GET', KEYS[1])
            if not stored then return 0 end
            local session = cjson.decode(stored)
            local expected = cjson.decode(ARGV[1])
            if expected ~= '' and session.token ~= expected then return 0 end
            if #ARGV == 1 then return redis.call('DEL', KEYS[1]) end
            session.principal = cjson.decode(ARGV[2])
            redis.call('SET', KEYS[1], cjson.encode(session), 'KEEPTTL')
            return 1
            """, Long.class);
    private static final RedisScript<Long> REVOKE_ALL = RedisScript.of("""
            local stored = redis.call('GET', KEYS[1])
            if not stored or cjson.decode(stored).token ~= cjson.decode(ARGV[1]) then return 0 end
            return redis.call('DEL', unpack(KEYS))
            """, Long.class);
    private static final RedisScript<Long> REVOKE_SUBJECT = RedisScript.of(
            "return redis.call('DEL', unpack(KEYS))", Long.class);
    private final ObjectProvider<ReactiveRedisOperation> redis;
    private final ObjectProvider<JwtSessionTokens> jwt;
    private final Cache<String, CachedSession> sessionCache = CacheBuilder.newBuilder()
            .maximumSize(32768)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();
    private final Cache<String, String> principalUserIdCache = CacheBuilder.newBuilder()
            .maximumSize(32768)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();
    private final AtomicLongArray invalidationEpoch = new AtomicLongArray(EPOCH_SLOTS);
    private final AtomicBoolean trackingConnecting = new AtomicBoolean(false);
    private final AtomicLong nextTrackingAttemptNanos = new AtomicLong(0L);
    private volatile StatefulRedisConnection<String, String> trackingConnection;
    private volatile boolean trackingActive;
    private volatile boolean disposed;

    PrincipalFactory(ObjectProvider<ReactiveRedisOperation> redis, ObjectProvider<JwtSessionTokens> jwt) {
        this.redis = redis;
        this.jwt = jwt;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (tryAcquireTrackingSlot()) {
            startTrackingSafely();
        }
    }

    @Override
    public void destroy() {
        disposed = true;
        trackingActive = false;
        var conn = trackingConnection;
        trackingConnection = null;
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception ex) {
                log.debug("Failed closing session tracking connection", ex);
            }
        }
        sessionCache.invalidateAll();
        principalUserIdCache.invalidateAll();
    }

    public Mono<String> currentUserId() {
        return currentIdentity().mapNotNull(identity -> {
            String token = identity.getAuthToken();
            if (token == null) {
                String principal = identity.getPrincipal();
                return principal == null ? null : extractUserId(principal);
            }
            if (!token.startsWith(LAMBDA_SECURITY_AUTH_TOKEN_KEY)) {
                String principal = identity.getPrincipal();
                if (principal == null) {
                    throw new EventException(CONTEXT_INVALID, "用户身份无法读取");
                }
                return extractUserId(principal);
            }
            int start = LAMBDA_SECURITY_AUTH_TOKEN_KEY.length();
            int end = token.lastIndexOf('.');
            return end > start ? token.substring(start, end) : null;
        });
    }

    private String extractUserId(String principal) {
        String cached = principalUserIdCache.getIfPresent(principal);
        if (cached != null) {
            return cached;
        }
        String id = readScopedPrincipal(principal).getId();
        if (id != null) {
            principalUserIdCache.put(principal, id);
        }
        return id;
    }

    /** Current user identifier for repositories with BSON ObjectId keys. */
    public Mono<ObjectId> currentUserObjectId() {
        return currentUserId().map(id -> {
            if (!ObjectId.isValid(id)) {
                throw new EventException(CONTEXT_INVALID, "用户ID不是有效的 ObjectId");
            }
            return new ObjectId(id);
        });
    }

    /** Returns the current request's user, or completes empty for an anonymous request. */
    public <T extends SecurityLoginUser<?>> Mono<T> currentUser(Class<T> type) {
        return currentIdentity().flatMap(identity -> Mono.justOrEmpty(identity.getPrincipal()))
                .map(principal -> JsonUtil.readRequired(principal, type,
                        () -> new EventException(CONTEXT_INVALID, "用户身份无法读取")));
    }

    public Mono<String> currentToken() {
        return currentIdentity().flatMap(identity -> Mono.justOrEmpty(identity.getAuthToken()));
    }

    private Mono<SecurityStash> currentIdentity() {
        return Mono.deferContextual(context -> Mono.justOrEmpty(context.<SecurityStash>getOrEmpty(SecurityStash.class)));
    }

    public Mono<String> resolvePrincipal(String token) {
        return Mono.defer(() -> {
            var parts = sessionRef(token);
            return readSession(parts);
        });
    }

    public <T extends SecurityLoginUser<?>> Mono<String> setPrincipal(T user) {
        return Mono.defer(() -> {
            if (user instanceof ScopedLoginUser scoped) {
                return issuePrincipal(scoped).map(IssuedPrincipal::token);
            }
            String key = sessionKey(user);
            String suffix = IdUtil.compactUuid();
            return storeSession(key, suffix, user, LAMBDA_SECURITY_TOKEN_TIME_SECOND).thenReturn(key + "." + suffix);
        });
    }

    public Mono<IssuedPrincipal> issuePrincipal(ScopedLoginUser user) {
        return Mono.defer(() -> {
            var issued = jwtTokens().issue(user);
            return storeSession(issued.session().key(), issued.session().id(), user, issued.ttl())
                    .thenReturn(new IssuedPrincipal(issued.token(), issued.expiresAt()));
        });
    }

    public <T extends SecurityLoginUser<?>> Mono<Void> updatePrincipal(T user) {
        return currentToken().flatMap(token -> {
            var parts = sessionRef(token);
            if (!parts.key().equals(keyFor(user))) {
                return Mono.error(invalidToken());
            }
            return changeSessionRequired(parts.key(), List.of(parts.suffix(), JsonUtil.write(user)));
        });
    }

    public <T extends SecurityLoginUser<?>> Mono<Void> revisePrincipal(T user) {
        return Mono.defer(() -> changeSession(keyFor(user), List.of("", JsonUtil.write(user))));
    }

    public Mono<Void> deletePrincipal() {
        return currentToken().flatMap(token -> {
            var parts = sessionRef(token);
            return changeSession(parts.key(), List.of(parts.suffix()));
        });
    }

    @SuppressWarnings("unchecked")
    public Mono<Void> deleteAllPrincipals() {
        return currentIdentity().flatMap(identity -> {
            String token = identity.getAuthToken();
            if (token == null) {
                return Mono.empty();
            }
            if (token.startsWith(LAMBDA_SECURITY_AUTH_TOKEN_KEY)) {
                return deletePrincipal();
            }
            var current = sessionRef(token);
            var scoped = readScopedPrincipal(identity.getPrincipal());
            var keys = new ArrayList<>(jwtTokens().keys(scoped));
            if (!keys.remove(current.key())) {
                return Mono.error(invalidToken());
            }
            keys.addFirst(current.key());
            return redis().execute(REVOKE_ALL, keys, List.of(current.suffix()))
                    .doOnSubscribe(ignored -> invalidateLocal(keys))
                    .doFinally(ignored -> invalidateLocal(keys))
                    .then();
        });
    }

    public Mono<Void> revokePrincipalsFor(ScopedLoginUser subject) {
        return Mono.defer(() -> {
            var keys = jwtTokens().keys(subject);
            return redis().execute(REVOKE_SUBJECT, keys, List.of())
                    .doOnSubscribe(ignored -> invalidateLocal(keys))
                    .doFinally(ignored -> invalidateLocal(keys))
                    .then();
        });
    }

    private Mono<Void> storeSession(String key, String id, SecurityLoginUser<?> user, long ttl) {
        var session = new Session(JsonUtil.write(user), id);
        return redis().set(key, session, ttl)
                .doOnSubscribe(ignored -> invalidateLocal(key))
                .doFinally(ignored -> invalidateLocal(key))
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(invalidToken())).then();
    }

    private String keyFor(SecurityLoginUser<?> user) {
        return user instanceof ScopedLoginUser scoped ? jwtTokens().key(scoped) : sessionKey(user);
    }

    private static ScopedLoginUser readScopedPrincipal(String principal) {
        return JsonUtil.readRequired(principal, ScopedLoginUser.class,
                () -> new EventException(CONTEXT_INVALID, "用户身份无法读取"));
    }

    private JwtSessionTokens jwtTokens() {
        var tokens = jwt.getIfAvailable();
        if (tokens == null) {
            throw invalidToken();
        }
        return tokens;
    }

    private TokenParts sessionRef(String token) {
        if (token != null && token.length() <= 1024 && !token.startsWith(LAMBDA_SECURITY_AUTH_TOKEN_KEY)) {
            var session = jwtTokens().verify(token);
            return new TokenParts(session.key(), session.id());
        }
        return parseToken(token);
    }

    private Mono<String> readSession(TokenParts token) {
        ensureTrackingStarted();
        if (trackingActive) {
            CachedSession cached = sessionCache.getIfPresent(token.key());
            if (cached != null) {
                if (MessageDigest.isEqual(cached.tokenBytes(),
                        token.suffix().getBytes(StandardCharsets.UTF_8))) {
                    return Mono.just(cached.principal());
                }
                return Mono.error(invalidToken());
            }
        }
        return Mono.defer(() -> {
            int slot = slotFor(token.key());
            long epochBefore = invalidationEpoch.get(slot);
            return redis().<String, Object>get(token.key()).<String>handle((value, sink) -> {
                if (value instanceof Map<?, ?> fields
                        && fields.get("token") instanceof String expected
                        && fields.get("principal") instanceof String principal) {
                    byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
                    if (trackingActive && invalidationEpoch.get(slot) == epochBefore) {
                        sessionCache.put(token.key(), new CachedSession(expectedBytes, principal));
                    }
                    if (MessageDigest.isEqual(expectedBytes,
                            token.suffix().getBytes(StandardCharsets.UTF_8))) {
                        sink.next(principal);
                    } else {
                        sink.error(invalidToken());
                    }
                } else {
                    sink.error(invalidToken());
                }
            }).switchIfEmpty(Mono.error(invalidToken()));
        });
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> changeSession(String key, List<String> arguments) {
        return redis().execute(CHANGE_SESSION, List.of(key), arguments)
                .doOnSubscribe(ignored -> invalidateLocal(key))
                .doFinally(ignored -> invalidateLocal(key))
                .then();
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> changeSessionRequired(String key, List<String> arguments) {
        return redis().execute(CHANGE_SESSION, List.of(key), arguments)
                .doOnSubscribe(ignored -> invalidateLocal(key))
                .doFinally(ignored -> invalidateLocal(key))
                .filter(result -> Long.valueOf(1L).equals(result))
                .switchIfEmpty(Mono.error(invalidToken()))
                .then();
    }

    private boolean tryAcquireTrackingSlot() {
        if (trackingActive || disposed) {
            return false;
        }
        var existing = trackingConnection;
        if (existing != null && existing.isOpen()) {
            return false;
        }
        long now = System.nanoTime();
        long nextAllowed = nextTrackingAttemptNanos.get();
        if (now < nextAllowed || !trackingConnecting.compareAndSet(false, true)) {
            return false;
        }
        nextTrackingAttemptNanos.set(now + TRACKING_RETRY_NANOS);
        return true;
    }

    private void ensureTrackingStarted() {
        if (!tryAcquireTrackingSlot()) {
            return;
        }
        var operation = redis.getIfAvailable();
        if (operation == null
                || !(operation.getConnectionFactory() instanceof LettuceConnectionFactory lettuce)
                || !(lettuce.getNativeClient() instanceof RedisClient)) {
            trackingConnecting.set(false);
            return;
        }
        Mono.fromRunnable(this::startTrackingSafely)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(ignored -> {}, error -> log.debug("Async session tracking setup failed", error));
    }

    private void startTrackingSafely() {
        try {
            var operation = redis.getIfAvailable();
            if (operation == null
                    || !(operation.getConnectionFactory() instanceof LettuceConnectionFactory lettuce)
                    || !(lettuce.getNativeClient() instanceof RedisClient client)) {
                return;
            }
            var tokens = jwt.getIfAvailable();
            String[] prefixes = tokens == null
                    ? new String[]{LAMBDA_SECURITY_AUTH_TOKEN_KEY}
                    : new String[]{LAMBDA_SECURITY_AUTH_TOKEN_KEY, tokens.trackingPrefix()};
            var trackingArgs = TrackingArgs.Builder.enabled().bcast().noloop().prefixes(prefixes);
            var conn = client.connect(StringCodec.UTF8);
            if (disposed) {
                conn.close();
                return;
            }
            trackingConnection = conn;
            conn.addListener((io.lettuce.core.api.push.PushListener) this::handleTrackingPush);
            conn.addListener(new RedisConnectionStateListener() {
                @Override
                public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
                    if (!disposed) {
                        enableTracking(conn, trackingArgs);
                    }
                }

                @Override
                public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
                    trackingActive = false;
                    invalidateAllLocal();
                }
            });
            enableTracking(conn, trackingArgs);
        } catch (Exception ex) {
            trackingActive = false;
            trackingConnection = null;
            log.debug("Failed to initialize Redis session tracking", ex);
        } finally {
            trackingConnecting.set(false);
        }
    }

    private void enableTracking(StatefulRedisConnection<String, String> conn, TrackingArgs args) {
        conn.async().clientTracking(args).whenComplete((reply, error) -> {
            if (disposed || error != null || !"OK".equalsIgnoreCase(reply)) {
                trackingActive = false;
                invalidateAllLocal();
                if (error != null) {
                    log.debug("Redis CLIENT TRACKING activation failed", error);
                }
                return;
            }
            invalidateAllLocal();
            trackingActive = true;
        });
    }

    private void handleTrackingPush(PushMessage message) {
        if (!"invalidate".equals(message.getType())) {
            return;
        }
        var content = message.getContent(buffer -> buffer == null ? null : StringCodec.UTF8.decodeKey(buffer));
        if (content == null || content.size() < 2) {
            invalidateAllLocal();
            return;
        }
        Object payload = content.get(1);
        if (payload instanceof List<?> keys) {
            for (Object entry : keys) {
                if (entry == null) {
                    invalidateAllLocal();
                    return;
                }
                invalidateLocal(entry.toString());
            }
        } else {
            invalidateAllLocal();
        }
    }

    private void invalidateLocal(String key) {
        if (key == null) {
            return;
        }
        invalidationEpoch.incrementAndGet(slotFor(key));
        sessionCache.invalidate(key);
    }

    private void invalidateLocal(List<String> keys) {
        if (keys == null) {
            return;
        }
        for (String key : keys) {
            invalidateLocal(key);
        }
    }

    private void invalidateAllLocal() {
        for (int i = 0; i < EPOCH_SLOTS; i++) {
            invalidationEpoch.incrementAndGet(i);
        }
        sessionCache.invalidateAll();
    }

    private static int slotFor(String key) {
        return (key.hashCode() & 0x7fffffff) & (EPOCH_SLOTS - 1);
    }

    private ReactiveRedisOperation redis() {
        var operation = redis.getIfAvailable();
        if (operation == null) {
            throw new EventException(ComplianceExceptionEnum.SESSION_CONFIGURATION_INVALID, "Configure Redis to store or verify user sessions");
        }
        return operation;
    }

    private static String sessionKey(SecurityLoginUser<?> user) {
        if (user == null || user.getId() == null || !USER_ID_FORMAT.matcher(user.getId().toString()).matches()) {
            throw new EventException(CONTEXT_INVALID, "用户ID必须由英文字母或数字组成");
        }
        return LAMBDA_SECURITY_AUTH_TOKEN_KEY + user.getId();
    }

    private static TokenParts parseToken(String token) {
        if (token == null || token.length() > 1024 || !TOKEN_FORMAT.matcher(token).matches()) {
            throw invalidToken();
        }
        int split = token.lastIndexOf('.');
        return new TokenParts(token.substring(0, split), token.substring(split + 1));
    }

    private static EventException invalidToken() {
        return new EventException(SESSION_INVALID, "无效令牌");
    }

    public record IssuedPrincipal(String token, Instant expiresAt) {}

    private record CachedSession(byte[] tokenBytes, String principal) {}
    private record Session(String principal, String token) {}
    private record TokenParts(String key, String suffix) {}
}
