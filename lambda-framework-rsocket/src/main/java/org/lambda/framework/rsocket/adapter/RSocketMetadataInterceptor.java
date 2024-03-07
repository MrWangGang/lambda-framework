/*
package org.lambda.framework.rsocket.adapter;

import io.rsocket.SocketAcceptor;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketStrategies;

public class RSocketMetadataInterceptor implements SocketAcceptorInterceptor {
    private MetadataExtractor metadataExtractor;
    public RSocketMetadataInterceptor(RSocketStrategies strategies) {
        this.metadataExtractor = strategies.metadataExtractor();
    }


    @Override
    public SocketAcceptor apply(SocketAcceptor socketAcceptor) {
        return (setup, sendingSocket) -> {
            setup.getMetadata();
            return socketAcceptor.accept(setup, sendingSocket);
        };
    }
}
*/
