package org.lambda.framework.rsocket.adapter;

import io.netty.buffer.ByteBuf;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import jakarta.annotation.Resource;
import org.lambda.framework.common.support.SecurityStash;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import static org.lambda.framework.common.enums.ConmonContract.AUTHTOKEN_STASH_NAMING;
import static org.lambda.framework.common.enums.ConmonContract.PRINCIPAL_STASH_NAMING;

@Component
public class RSocketAcceptorInterceptor implements SocketAcceptorInterceptor {

    @Override
    public SocketAcceptor apply(SocketAcceptor socketAcceptor) {
        return (setup, sendingSocket) -> {
            SecurityStash securityStash = getValueFromMetadata(setup.metadata());
            return socketAcceptor.accept(setup,sendingSocket).map(rsocket->{
                return createRSocket(rsocket,securityStash);
            });
        };
    }

    private SecurityStash getValueFromMetadata(ByteBuf metadata) {
        SecurityStash securityStash = SecurityStash.builder().build();
        if(metadata!=null){
            CompositeMetadata compositeMetadata = new CompositeMetadata(metadata, false);
            if(compositeMetadata!=null){
                compositeMetadata.stream()
                        .forEach(entry -> {
                            if(AUTHTOKEN_STASH_NAMING.equals(entry.getMimeType())){
                                ByteBuf content = entry.getContent();
                                String _authtoken = content.toString(io.netty.util.CharsetUtil.UTF_8);
                                securityStash.setAuthToken(_authtoken);
                            }
                            if(PRINCIPAL_STASH_NAMING.equals(entry.getMimeType())){
                                ByteBuf content = entry.getContent();
                                String _principal = content.toString(io.netty.util.CharsetUtil.UTF_8);
                                securityStash.setPrincipal(_principal);
                            }
                        });
                return securityStash;
            }
        }
        return securityStash;
    }

    @Resource
    private RsocketGlobalExceptionHandler rsocketGlobalExceptionHandler;
    private RSocket createRSocket(RSocket sendingSocket,SecurityStash securityStash) {
        return new RSocket() {
            @Override
            public Mono<Void> fireAndForget(Payload payload) {
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
                return sendingSocket.requestChannel(payloads)
                        .onErrorResume(throwable -> {
                            // 自定义异常处理逻辑
                            throwable = rsocketGlobalExceptionHandler.handleException(throwable);
                            return Mono.error(throwable); // 可以选择返回错误或者其他逻辑
                        })
                        .contextWrite(Context.of(SecurityStash.class,securityStash));

            }
            @Override
            public Mono<Void> metadataPush(Payload payload) {
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
