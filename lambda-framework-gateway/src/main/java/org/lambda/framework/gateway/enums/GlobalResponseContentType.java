package org.lambda.framework.gateway.enums;

import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

public class GlobalResponseContentType {
    public static MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

}
