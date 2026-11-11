package org.lambda.framework.rsocket.adapter;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.SocketAcceptor;
import io.rsocket.exceptions.RejectedSetupException;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import org.lambda.framework.common.support.GatewayRouteContract;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

final class GatewayRouteVersionInterceptor implements SocketAcceptorInterceptor {
    private final ObjectProvider<GatewayRouteContract> routes;

    GatewayRouteVersionInterceptor(ObjectProvider<GatewayRouteContract> routes) {
        this.routes = routes;
    }

    @Override
    public SocketAcceptor apply(SocketAcceptor acceptor) {
        return (setup, sender) -> {
            if (!matches(setup)) {
                return Mono.error(new RejectedSetupException("RSocket gateway route contract changed"));
            }
            return acceptor.accept(setup, sender);
        };
    }

    private boolean matches(ConnectionSetupPayload setup) {
        if (!setup.hasMetadata() || !WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString()
                .equals(setup.metadataMimeType())) {
            return true;
        }
        boolean versionSeen = false;
        try {
            for (var entry : new CompositeMetadata(setup.metadata(), false)) {
                if (GatewayRouteContract.SETUP_MIME_TYPE.equals(entry.getMimeType())) {
                    var content = entry.getContent();
                    if (versionSeen || content.readableBytes() != 64) {
                        return false;
                    }
                    var contract = routes.getIfAvailable();
                    if (contract == null || !contract.version().equals(content.toString(StandardCharsets.US_ASCII))) {
                        return false;
                    }
                    versionSeen = true;
                }
            }
            return true;
        } catch (IndexOutOfBoundsException | IllegalArgumentException error) {
            return false;
        }
    }
}
