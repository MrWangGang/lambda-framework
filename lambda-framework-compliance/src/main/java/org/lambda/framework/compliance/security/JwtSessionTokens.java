package org.lambda.framework.compliance.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.po.ScopedLoginUser;
import org.lambda.framework.common.po.ScopedLoginUser.Realm;
import org.lambda.framework.common.po.ScopedLoginUser.Terminal;
import org.lambda.framework.common.util.IdUtil;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import static org.lambda.framework.enums.ComplianceExceptionEnum.CONTEXT_INVALID;
import static org.lambda.framework.enums.ComplianceExceptionEnum.SESSION_INVALID;

final class JwtSessionTokens {
    private static final Pattern USER_ID = Pattern.compile("[0-9a-f]{24}");
    private static final Pattern SESSION_ID = Pattern.compile("[0-9a-f]{32}");
    private static final String TYPE = "lambda-session+jwt";
    private static final Duration MAX_APP_LIFETIME = Duration.ofDays(30);
    private final JwtSessionSettings settings;
    private final Duration appLifetime;
    private final JwtDecoder decoder;
    private final JwtEncoder encoder;
    private final Map<String, RSAPublicKey> publicKeys;

    JwtSessionTokens(JwtSessionSettings settings) {
        this.settings = settings;
        appLifetime = settings.appLifetime() == null ? settings.lifetime() : settings.appLifetime();
        if (blank(settings.issuer()) || blank(settings.appAudience()) || blank(settings.adminAudience())
                || settings.appAudience().equals(settings.adminAudience()) || settings.publicKeys() == null
                || settings.publicKeys().isEmpty() || settings.lifetime().getSeconds() < 60
                || settings.lifetime().toSeconds() > 3600 || appLifetime.getSeconds() < 60
                || appLifetime.compareTo(MAX_APP_LIFETIME) > 0 || !settings.redisPrefix().matches("[a-zA-Z0-9:_-]+")) {
            throw new IllegalArgumentException("Invalid JWT session configuration");
        }
        publicKeys = settings.publicKeys().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> publicKey(entry.getValue())));
        var keys = publicKeys.entrySet().stream()
                .<JWK>map(entry -> new RSAKey.Builder(entry.getValue()).keyID(entry.getKey()).build()).toList();
        var verifier = NimbusJwtDecoder.withJwkSource(new ImmutableJWKSet<>(new JWKSet(keys)))
                .jwsAlgorithm(SignatureAlgorithm.RS256).jwtProcessorCustomizer(processor ->
                        processor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType(TYPE)))).build();
        verifier.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator(settings.issuer()), new JwtTypeValidator(TYPE)));
        decoder = verifier;
        encoder = signingEncoder();
    }

    Issued issue(ScopedLoginUser user) {
        if (encoder == null) {
            throw new IllegalStateException("JWT signing key is not configured");
        }
        var key = key(user);
        var sessionId = IdUtil.compactUuid();
        var now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
        var lifetime = user.getRealm() == Realm.APP ? appLifetime : settings.lifetime();
        var expiresAt = now.plus(lifetime);
        var claims = JwtClaimsSet.builder().issuer(settings.issuer()).subject(user.getId())
                .audience(List.of(audience(user.getRealm()))).issuedAt(now).expiresAt(expiresAt)
                .id(sessionId).claim("realm", user.getRealm().name()).claim("terminal", user.getTerminal().code()).build();
        var headers = JwsHeader.with(SignatureAlgorithm.RS256).keyId(settings.signingKeyId()).type(TYPE).build();
        var token = encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
        return new Issued(token, new SessionRef(key, sessionId), lifetime.getSeconds(), expiresAt);
    }

    SessionRef verify(String token) {
        try {
            var jwt = decoder.decode(token);
            var realm = Realm.valueOf(jwt.getClaimAsString("realm"));
            String terminalCode = jwt.getClaimAsString("terminal");
            var terminal = Arrays.stream(Terminal.values()).filter(item -> item.code().equals(terminalCode))
                    .findFirst().orElseThrow(JwtSessionTokens::invalid);
            var now = Instant.now();
            if (!(jwt.getHeaders().get("kid") instanceof String keyId) || !publicKeys.containsKey(keyId)
                    || !List.of(audience(realm)).equals(jwt.getAudience()) || !validId(jwt.getSubject())
                    || jwt.getId() == null || !SESSION_ID.matcher(jwt.getId()).matches()
                    || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(now) || jwt.getIssuedAt() == null
                    || jwt.getIssuedAt().isAfter(now.plusSeconds(5)) || !jwt.getIssuedAt().isBefore(jwt.getExpiresAt())
                    || !terminal.supports(realm)) {
                throw invalid();
            }
            return new SessionRef(key(new ScopedLoginUser(jwt.getSubject(), realm, terminal)), jwt.getId());
        } catch (JwtException | IllegalArgumentException | NullPointerException failure) {
            throw invalid();
        }
    }

    String key(ScopedLoginUser user) {
        if (!validId(user.getId()) || user.getTerminal() == null || !user.getTerminal().supports(user.getRealm())) {
            throw new EventException(CONTEXT_INVALID, "会话身份无效");
        }
        return prefix(user) + user.getTerminal().code();
    }

    List<String> keys(ScopedLoginUser user) {
        key(user);
        return Arrays.stream(Terminal.values()).filter(terminal -> terminal.supports(user.getRealm()))
                .map(terminal -> prefix(user) + terminal.code()).toList();
    }

    private String prefix(ScopedLoginUser user) {
        return settings.redisPrefix() + ":{" + user.getRealm().name().toLowerCase(Locale.ROOT)
                + ":" + user.getId() + "}:";
    }

    private String audience(Realm realm) {
        return realm == Realm.APP ? settings.appAudience() : settings.adminAudience();
    }

    private JwtEncoder signingEncoder() {
        if (settings.privateKey() == null && blank(settings.signingKeyId())) {
            return null;
        }
        var publicKey = publicKeys.get(settings.signingKeyId());
        if (publicKey == null || settings.privateKey() == null) {
            throw new IllegalArgumentException("JWT signing key requires a matching verification key");
        }
        try (var input = settings.privateKey().getInputStream()) {
            RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(input);
            if (privateKey == null || !privateKey.getModulus().equals(publicKey.getModulus())) {
                throw new IllegalArgumentException("JWT signing and verification keys do not match");
            }
            var key = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(settings.signingKeyId()).build();
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        } catch (IOException failure) {
            throw new IllegalArgumentException("Cannot load JWT signing key", failure);
        }
    }

    private static RSAPublicKey publicKey(Resource resource) {
        try (var input = resource.getInputStream()) {
            RSAPublicKey key = RsaKeyConverters.x509().convert(input);
            if (key == null || key.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException("JWT verification key must be at least 2048 bits");
            }
            return key;
        } catch (IOException failure) {
            throw new IllegalArgumentException("Cannot load JWT verification key", failure);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean validId(String id) {
        return id != null && USER_ID.matcher(id).matches();
    }

    private static EventException invalid() {
        return new EventException(SESSION_INVALID, "无效令牌");
    }

    record SessionRef(String key, String id) {}

    record Issued(String token, SessionRef session, long ttl, Instant expiresAt) {}
}
