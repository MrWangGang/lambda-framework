package org.lambda.framework.rsocket.adapter;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

@Component
public class RSocketSocketAccpInterceptor implements SocketAcceptorInterceptor {

    @Override
    public SocketAcceptor apply(SocketAcceptor socketAcceptor) {
        return (setup, sendingSocket) -> {
            // 在这里添加你的自定义逻辑
            // ...

            // 返回经过修改的或原始的 SocketAcceptor
            return socketAcceptor.accept(setup,createRSocket(sendingSocket));
        };
    }

    private RSocket createRSocket(RSocket sendingSocket) {
        return new RSocket() {
            @Override
            public Flux<Payload> requestStream(Payload payload) {
                return sendingSocket.requestStream(payload)
                        .contextWrite(Context.of("ddd","ddd"));
            }
        };
    }
}
