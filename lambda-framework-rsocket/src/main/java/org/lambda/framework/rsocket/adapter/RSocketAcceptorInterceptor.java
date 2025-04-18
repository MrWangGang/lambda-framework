package org.lambda.framework.rsocket.adapter;

import io.netty.buffer.ByteBuf;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.SecurityStash;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.lambda.framework.rsocket.enums.RsocketExceptionEnum.ES_RSOCKET_003;

@Component
public class RSocketAcceptorInterceptor implements SocketAcceptorInterceptor {

    @Override
    public SocketAcceptor apply(SocketAcceptor socketAcceptor) {
        return (setup, sendingSocket) -> {
            return socketAcceptor.accept(setup,sendingSocket).map(rsocket->{
                return createRSocket(rsocket);
            });
        };
    }



    private SecurityStash getValueFromMetadata(ByteBuffer metadata)  {
        // 空判断：ByteBuffer 没数据时返回默认对象
        if (metadata == null || !metadata.hasRemaining()) {
            return SecurityStash.builder().build();
        }

        // 将 ByteBuffer 转成 Netty 的 ByteBuf 以供 CompositeMetadata 使用
        ByteBuf byteBuf = io.netty.buffer.Unpooled.wrappedBuffer(metadata);

        CompositeMetadata compositeMetadata = new CompositeMetadata(byteBuf, false);
        for (CompositeMetadata.Entry entry : compositeMetadata) {
            String mimeType = entry.getMimeType();
            if(Assert.verify(mimeType)){
                // 你这边可能就是用的 application/json 存的 security
                if (MimeTypeUtils.APPLICATION_JSON.isCompatibleWith(MimeType.valueOf(mimeType))) {
                    String json = entry.getContent().toString(StandardCharsets.UTF_8);
                    Optional<SecurityStash> obj = JsonUtil.stringToObj(json, SecurityStash.class);
                    return obj.orElseThrow(() -> new EventException(ES_RSOCKET_003));
                }
            }
        }
        throw new EventException(ES_RSOCKET_003);
    }

    @Resource
    private RsocketGlobalExceptionHandler rsocketGlobalExceptionHandler;
    /*创建每个新的 RSocket 实例本身并不会特别耗费性能，因为 RSocket 本身是一个轻量级的协议实现，
    不会在创建实例时引入显著的性能开销。但是，频繁地创建和销毁连接可能会对性能产生一定的影响，
    这主要取决于底层传输层的实现和连接的复用情况。和httpclient不同的是，rsocket底层复用了链接，httpclient是链接池
    所以这只是简单的new一个对象的操作，而没有去创建和销毁链接，链接是rsocket底层去维护和复用的 rsocket接口只是告诉
    你们如何去发消息
    */
    private RSocket createRSocket(RSocket sendingSocket) {
        return new RSocket() {
            @Override
            public Mono<Void> fireAndForget(Payload payload) {
                SecurityStash securityStash;
                securityStash = getValueFromMetadata(payload.getMetadata());
                return sendingSocket.fireAndForget(payload)
                        .onErrorResume(throwable -> {
                            // 自定义异常处理逻辑
                            throwable = rsocketGlobalExceptionHandler.handleException(throwable);
                            return Mono.error(throwable); // 可以选择返回错误或者其他逻辑
                        })
                        .contextWrite(Context.of(SecurityStash.class,securityStash));
            }
            @Override
            public Mono<Payload> requestResponse(Payload payload) {
                SecurityStash securityStash;
                securityStash = getValueFromMetadata(payload.getMetadata());
                return sendingSocket.requestResponse(payload)
                        .onErrorResume(throwable -> {
                            // 自定义异常处理逻辑
                            throwable = rsocketGlobalExceptionHandler.handleException(throwable);
                            return Mono.error(throwable); // 可以选择返回错误或者其他逻辑
                        })
                        .contextWrite(Context.of(SecurityStash.class,securityStash));
            }
            @Override
            public Flux<Payload> requestStream(Payload payload) {
                SecurityStash securityStash;
                securityStash = getValueFromMetadata(payload.getMetadata());
                return sendingSocket.requestStream(payload)
                        .onErrorResume(throwable -> {
                            // 自定义异常处理逻辑
                            throwable = rsocketGlobalExceptionHandler.handleException(throwable);
                            return Mono.error(throwable); // 可以选择返回错误或者其他逻辑
                        })
                        .contextWrite(Context.of(SecurityStash.class,securityStash));
            }
            @Override
            public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                return Flux.from(payloads)
                        .switchOnFirst((signal, flux) -> {
                            Payload first = signal.get();
                            if (first == null) {
                                return Flux.error(new EventException(ES_RSOCKET_003)); // 空流处理
                            }
                            SecurityStash stash;
                            stash = getValueFromMetadata(first.getMetadata());
                            return sendingSocket.requestChannel(flux)
                                    .onErrorResume(throwable -> {
                                        throwable = rsocketGlobalExceptionHandler.handleException(throwable);
                                        return Mono.error(throwable);
                                    })
                                    .contextWrite(Context.of(SecurityStash.class, stash));
                        });
            }

            @Override
            public Mono<Void> metadataPush(Payload payload) {
                SecurityStash securityStash;
                securityStash = getValueFromMetadata(payload.getMetadata());
                return sendingSocket.metadataPush(payload)
                        .onErrorResume(throwable -> {
                            // 自定义异常处理逻辑
                            throwable = rsocketGlobalExceptionHandler.handleException(throwable);
                            return Mono.error(throwable); // 可以选择返回错误或者其他逻辑
                        })
                        .contextWrite(Context.of(SecurityStash.class,securityStash));

            }
            @Override
            public double availability() {
                return sendingSocket.availability();
            }

            @Override
            public void dispose() {
                sendingSocket.dispose();
            }

            @Override
            public boolean isDisposed() {
                return sendingSocket.isDisposed();
            }

            @Override
            public Mono<Void> onClose() {
                return sendingSocket.onClose();
            }
        };
    }
}
