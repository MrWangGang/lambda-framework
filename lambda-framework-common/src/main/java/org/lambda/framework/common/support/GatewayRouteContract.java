package org.lambda.framework.common.support;

import org.lambda.framework.common.enums.GatewayInteraction;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

@FunctionalInterface
public interface GatewayRouteContract {
    String METADATA_KEY = "lambda.gateway.route-version";
    String SETUP_MIME_TYPE = "application/x-lambda-route-version";

    String version();

    static String versionOf(Map<String, GatewayInteraction> routes) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            new TreeMap<>(routes).forEach((path, interaction) -> {
                var bytes = path.getBytes(StandardCharsets.UTF_8);
                digest.update((bytes.length + ":").getBytes(StandardCharsets.US_ASCII));
                digest.update(bytes);
                digest.update(interaction.name().getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) 0);
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }
}
