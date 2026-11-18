package org.lambda.framework.rsocket.codec;

import org.lambda.framework.common.enums.RSocketContract;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.SecurityStash;
import org.lambda.framework.enums.RSocketExceptionEnum;
import org.lambda.framework.rsocket.adapter.ForyRpcTypes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import org.lambda.framework.common.util.JsonUtil;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.rsocket.autoconfigure.RSocketStrategiesAutoConfiguration;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoConfiguration(before = RSocketStrategiesAutoConfiguration.class)
public class RSocketStrategiesConfig implements BeanFactoryAware {
    private final List<ForyRpcTypes> foryTypes;
    private ListableBeanFactory beans;

    public RSocketStrategiesConfig() {
        this(List.of());
    }

    @Autowired
    public RSocketStrategiesConfig(List<ForyRpcTypes> foryTypes) {
        this.foryTypes = foryTypes;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (beanFactory instanceof ListableBeanFactory listable) {
            beans = listable;
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public RSocketStrategies rSocketStrategies() {
        var mapper = JsonUtil.mapper();
        var builder = RSocketStrategies.builder()
                .routeMatcher(new PathPatternRouteMatcher())
                .decoder(new Jackson2JsonDecoder(mapper))
                .encoder(new RSocketJsonEncoder(mapper))
                // Spring's default StringEncoder sends raw text, even on a JSON RSocket connection.
                .encoders(encoders -> encoders.addFirst(new RSocketJsonEncoder(mapper) {
                    @Override
                    public boolean canEncode(ResolvableType type, MimeType mimeType) {
                        return type.toClass() == String.class && MimeTypeUtils.APPLICATION_JSON.isCompatibleWith(mimeType);
                    }
                }))
                .decoders(decoders -> decoders.addFirst(new JsonStringDecoder(
                        mapper.readerFor(String.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS))));
        var codec = new ForyRpcCodec(foryTypes, () -> beans == null ? Set.of() : ForyRpcContracts.discover(beans));
        builder.encoders(encoders -> {
                    encoders.addFirst(new IdentityStashEncoder());
                    encoders.addFirst(codec.encoder());
                })
                .decoders(decoders -> {
                    decoders.addFirst(new IdentityStashDecoder());
                    decoders.addFirst(codec.decoder());
                });
        return builder.build();
    }

    private static final class IdentityStashEncoder extends AbstractEncoder<SecurityStash> {
        private static final MimeType IDENTITY_MIME = MimeType.valueOf(RSocketContract.IDENTITY_MIME_TYPE);
        private static final byte[] EMPTY = new byte[0];

        private IdentityStashEncoder() {
            super(IDENTITY_MIME);
        }

        @Override
        public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
            return mimeType != null && IDENTITY_MIME.equalsTypeAndSubtype(mimeType)
                    && SecurityStash.class.isAssignableFrom(elementType.toClass());
        }

        @Override
        public DataBuffer encodeValue(SecurityStash value, DataBufferFactory factory, ResolvableType valueType,
                                      MimeType mimeType, Map<String, Object> hints) {
            byte[] token = value != null && value.getAuthToken() != null
                    ? value.getAuthToken().getBytes(StandardCharsets.UTF_8) : EMPTY;
            if (token.length > 65535) {
                throw new EventException(RSocketExceptionEnum.INVALID_IDENTITY, "Auth token exceeds binary header limit");
            }
            byte[] principal = value != null && value.getPrincipal() != null
                    ? value.getPrincipal().getBytes(StandardCharsets.UTF_8) : EMPTY;
            DataBuffer buffer = factory.allocateBuffer(2 + token.length + principal.length);
            boolean success = false;
            try {
                buffer.write((byte) (token.length >>> 8));
                buffer.write((byte) token.length);
                if (token.length > 0) {
                    buffer.write(token);
                }
                if (principal.length > 0) {
                    buffer.write(principal);
                }
                success = true;
                return buffer;
            } finally {
                if (!success) {
                    DataBufferUtils.release(buffer);
                }
            }
        }

        @Override
        public Flux<DataBuffer> encode(Publisher<? extends SecurityStash> inputStream, DataBufferFactory bufferFactory,
                                       ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
            return Flux.from(inputStream).map(item -> encodeValue(item, bufferFactory, elementType, mimeType, hints));
        }
    }

    private static final class IdentityStashDecoder extends AbstractDecoder<SecurityStash> {
        private static final MimeType IDENTITY_MIME = MimeType.valueOf(RSocketContract.IDENTITY_MIME_TYPE);

        private IdentityStashDecoder() {
            super(IDENTITY_MIME);
        }

        @Override
        public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
            return mimeType != null && IDENTITY_MIME.equalsTypeAndSubtype(mimeType)
                    && elementType.toClass().isAssignableFrom(SecurityStash.class);
        }

        @Override
        public SecurityStash decode(DataBuffer buffer, ResolvableType targetType, MimeType mimeType,
                                    Map<String, Object> hints) {
            try {
                int readable = buffer.readableByteCount();
                if (readable < 2) {
                    throw new EventException(RSocketExceptionEnum.INVALID_IDENTITY);
                }
                int tokenLen = ((buffer.read() & 0xFF) << 8) | (buffer.read() & 0xFF);
                if (tokenLen > readable - 2) {
                    throw new EventException(RSocketExceptionEnum.INVALID_IDENTITY);
                }
                String token = tokenLen == 0 ? null : readString(buffer, tokenLen);
                int principalLen = readable - 2 - tokenLen;
                String principal = principalLen == 0 ? null : readString(buffer, principalLen);
                return new SecurityStash(token, principal);
            } finally {
                DataBufferUtils.release(buffer);
            }
        }

        private static String readString(DataBuffer buffer, int length) {
            byte[] bytes = new byte[length];
            buffer.read(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @Override
        public Flux<SecurityStash> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
                                          MimeType mimeType, Map<String, Object> hints) {
            return Flux.from(inputStream).map(buf -> decode(buf, elementType, mimeType, hints))
                    .doOnDiscard(DataBuffer.class, DataBufferUtils::release);
        }
    }

    private static final class JsonStringDecoder extends AbstractDecoder<String> {
        private final ObjectReader reader;

        private JsonStringDecoder(ObjectReader reader) {
            super(MimeTypeUtils.APPLICATION_JSON);
            this.reader = reader;
        }

        @Override
        public boolean canDecode(ResolvableType type, MimeType mimeType) {
            return type.toClass() == String.class && MimeTypeUtils.APPLICATION_JSON.isCompatibleWith(mimeType);
        }

        @Override
        public String decode(DataBuffer buffer, ResolvableType type, MimeType mimeType, Map<String, Object> hints) {
            String text;
            try {
                text = buffer.toString(StandardCharsets.UTF_8);
            } finally {
                DataBufferUtils.release(buffer);
            }
            if (text.startsWith("\"") && text.endsWith("\"")) {
                try {
                    return reader.readValue(text);
                } catch (JsonProcessingException ignored) {
                    // A rolling upgrade can still receive a legacy raw String from another service.
                }
            }
            return text;
        }

        @Override
        public Flux<String> decode(Publisher<DataBuffer> buffers, ResolvableType type, MimeType mimeType,
                                   Map<String, Object> hints) {
            return Flux.from(buffers).map(buffer -> decode(buffer, type, mimeType, hints))
                    .doOnDiscard(DataBuffer.class, DataBufferUtils::release);
        }
    }
}
