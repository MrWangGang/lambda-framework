package org.lambda.framework.compliance.security;

import org.bson.types.ObjectId;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.po.ScopedLoginUser;
import org.lambda.framework.common.po.SecurityLoginUser;
import org.lambda.framework.common.support.SecurityStash;
import org.lambda.framework.common.util.JsonUtil;
import org.lambda.framework.common.util.IdUtil;
import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.lambda.framework.common.enums.ConmonContract.LAMBDA_SECURITY_AUTH_TOKEN_KEY;
import static org.lambda.framework.common.enums.ConmonContract.LAMBDA_SECURITY_AUTH_TOKEN_REGEX;
import static org.lambda.framework.common.enums.ConmonContract.LAMBDA_SECURITY_TOKEN_TIME_SECOND;
import static org.lambda.framework.enums.ComplianceExceptionEnum.CONTEXT_INVALID;
import static org.lambda.framework.enums.ComplianceExceptionEnum.SESSION_INVALID;

public final class PrincipalFactory {
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
    private final ObjectProvider<ReactiveRedisOperation> redis;
    private final ObjectProvider<JwtSessionTokens> jwt;

    PrincipalFactory(ObjectProvider<ReactiveRedisOperation> redis, ObjectProvider<JwtSessionTokens> jwt) {
        this.redis = redis;
        this.jwt = jwt;
    }

    public Mono<String> currentUserId() {
        return currentIdentity().mapNotNull(identity -> {
            String token = identity.getAuthToken();
            if (token == null) {
                return null;
            }
            if (!token.startsWith(LAMBDA_SECURITY_AUTH_TOKEN_KEY)) {
                return readScopedPrincipal(identity.getPrincipal()).getId();
            }
            int start = LAMBDA_SECURITY_AUTH_TOKEN_KEY.length();
            int end = token.lastIndexOf('.');
            return token.startsWith(LAMBDA_SECURITY_AUTH_TOKEN_KEY) && end > start
                    ? token.substring(start, end) : null;
        });
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
            return changeSession(parts.key(), List.of(parts.suffix(), JsonUtil.write(user)));
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
            return redis().execute(REVOKE_ALL, keys, List.of(current.suffix())).then();
        });
    }

    private Mono<Void> storeSession(String key, String id, SecurityLoginUser<?> user, long ttl) {
        var session = new Session(JsonUtil.write(user), id);
        return redis().set(key, session, ttl).filter(Boolean.TRUE::equals)
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
        return redis().<String, Object>get(token.key()).<String>handle((value, sink) -> {
            if (value instanceof Map<?, ?> fields
                    && fields.get("token") instanceof String expected
                    && fields.get("principal") instanceof String principal
                    && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                            token.suffix().getBytes(StandardCharsets.UTF_8))) {
                sink.next(principal);
            } else {
                sink.error(invalidToken());
            }
        }).switchIfEmpty(Mono.error(invalidToken()));
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> changeSession(String key, List<String> arguments) {
        return redis().execute(CHANGE_SESSION, List.of(key), arguments).then();
    }

    private ReactiveRedisOperation redis() {
        var operation = redis.getIfAvailable();
        if (operation == null) {
            throw new IllegalStateException("Configure Redis to store or verify user sessions");
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

    private record Session(String principal, String token) {}
    private record TokenParts(String key, String suffix) {}
}
