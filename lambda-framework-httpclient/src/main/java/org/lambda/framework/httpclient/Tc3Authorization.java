package org.lambda.framework.httpclient;


import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.HttpClientExceptionEnum;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Tc3Authorization {
    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final String SIGNED_HEADERS = "content-type;host";
    private static final String TERMINATOR = "tc3_request";
    private static final HexFormat HEX = HexFormat.of();

    private Tc3Authorization() {}

    public static String postJson(String host, String service, String contentType, String payload,
                                  Instant at, String secretId, String secretKey) {
        return postJson(host, service, "/", contentType, payload, at, secretId, secretKey);
    }

    public static String postJson(String host, String service, String canonicalUri, String contentType, String payload,
                                  Instant at, String secretId, String secretKey) {
        try {
            String date = LocalDate.ofInstant(at, ZoneOffset.UTC).toString();
            String scope = date + "/" + service + "/" + TERMINATOR;
            String normalizedContentType = contentType.trim().toLowerCase(Locale.ROOT);
            String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
            String path = (canonicalUri == null || canonicalUri.isBlank()) ? "/" : canonicalUri;
            String headers = "content-type:" + normalizedContentType + "\nhost:" + normalizedHost + "\n";
            String canonical = String.join("\n", "POST", path, "", headers, SIGNED_HEADERS, digest(payload));
            String input = String.join("\n", ALGORITHM, Long.toString(at.getEpochSecond()), scope, digest(canonical));
            byte[] dateKey = hmac(bytes("TC3" + secretKey), date);
            byte[] serviceKey = hmac(dateKey, service);
            byte[] signingKey = hmac(serviceKey, TERMINATOR);
            return ALGORITHM + " Credential=" + secretId + "/" + scope + ", SignedHeaders=" + SIGNED_HEADERS
                    + ", Signature=" + HEX.formatHex(hmac(signingKey, input));
        } catch (GeneralSecurityException failure) {
            throw new EventException(HttpClientExceptionEnum.REQUEST_SIGNING_FAILED, "Unable to sign Tencent Cloud request", failure);
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] hmac(byte[] key, String value) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(bytes(value));
    }

    private static String digest(String value) throws GeneralSecurityException {
        return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(bytes(value)));
    }
}
