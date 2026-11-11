package org.lambda.framework.rsocket.codec;

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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
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
        builder.encoders(encoders -> encoders.addFirst(codec.encoder()))
                .decoders(decoders -> decoders.addFirst(codec.decoder()));
        return builder.build();
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
