package org.lambda.framework.rsocket.adapter;

import org.apache.fory.Fory;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.config.Config;
import org.apache.fory.context.ReadContext;
import org.apache.fory.context.WriteContext;
import org.apache.fory.serializer.Serializer;
import org.bson.types.ObjectId;
import org.lambda.framework.common.enums.RSocketContract;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.function.SingletonSupplier;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class ForyRpcCodec {
    private static final MimeType MIME_TYPE = MimeType.valueOf(RSocketContract.FORY_MIME_TYPE);
    private final SingletonSupplier<ThreadSafeFory> fory;

    ForyRpcCodec(List<ForyRpcTypes> groups, Supplier<Set<Class<?>>> contracts) {
        fory = SingletonSupplier.of(() -> create(groups, contracts.get()));
    }

    private static ThreadSafeFory create(List<ForyRpcTypes> groups, Set<Class<?>> contractTypes) {
        var fory = Fory.builder().withXlang(false).withCompatible(true).requireClassRegistration(true)
                .withDeserializeUnknownClass(false).serializeEnumByName(true).buildThreadSafeFory();
        fory.registerSerializer(ObjectId.class, resolver -> new ObjectIdSerializer(resolver.getConfig()));
        Stream.concat(contractTypes.stream(), groups.stream().flatMap(group -> group.types().stream())).distinct()
                .filter(type -> type != ObjectId.class).forEach(type -> fory.register(type, type.getName()));
        return fory;
    }

    AbstractEncoder<Object> encoder() {
        return new AbstractEncoder<>(MIME_TYPE) {
            @Override
            public boolean canEncode(ResolvableType type, MimeType mimeType) {
                return mimeType != null && MIME_TYPE.equalsTypeAndSubtype(mimeType);
            }

            @Override
            public DataBuffer encodeValue(Object value, DataBufferFactory factory, ResolvableType type,
                                          MimeType mimeType, Map<String, Object> hints) {
                try {
                    return factory.wrap(fory.obtain().serialize(value));
                } catch (RuntimeException error) {
                    throw new EncodingException("Cannot encode Fory RPC payload", error);
                }
            }

            @Override
            public Flux<DataBuffer> encode(Publisher<?> values, DataBufferFactory factory, ResolvableType type,
                                           MimeType mimeType, Map<String, Object> hints) {
                return Flux.from(values).map(value -> encodeValue(value, factory, type, mimeType, hints));
            }
        };
    }

    AbstractDecoder<Object> decoder() {
        return new AbstractDecoder<>(MIME_TYPE) {
            @Override
            public boolean canDecode(ResolvableType type, MimeType mimeType) {
                return mimeType != null && MIME_TYPE.equalsTypeAndSubtype(mimeType);
            }

            @Override
            public Object decode(DataBuffer input, ResolvableType type, MimeType mimeType, Map<String, Object> hints) {
                Object decoded;
                try {
                    byte[] bytes = new byte[input.readableByteCount()];
                    input.read(bytes);
                    decoded = fory.obtain().deserialize(bytes);
                } catch (RuntimeException error) {
                    throw new DecodingException("Cannot decode Fory RPC payload", error);
                } finally {
                    DataBufferUtils.release(input);
                }
                if (decoded == null || !ClassUtils.isAssignableValue(type.toClass(), decoded)) {
                    throw new DecodingException("Fory RPC payload does not match " + type);
                }
                return decoded;
            }

            @Override
            public Flux<Object> decode(Publisher<DataBuffer> buffers, ResolvableType type, MimeType mimeType,
                                       Map<String, Object> hints) {
                return Flux.from(buffers).map(buffer -> decode(buffer, type, mimeType, hints))
                        .doOnDiscard(DataBuffer.class, DataBufferUtils::release);
            }
        };
    }

    private static final class ObjectIdSerializer extends Serializer<ObjectId> {
        private ObjectIdSerializer(Config config) {
            super(config, ObjectId.class);
        }

        @Override
        public void write(WriteContext context, ObjectId value) {
            context.getBuffer().writeBytes(value.toByteArray());
        }

        @Override
        public ObjectId read(ReadContext context) {
            return new ObjectId(context.getBuffer().readBytes(12));
        }
    }
}
