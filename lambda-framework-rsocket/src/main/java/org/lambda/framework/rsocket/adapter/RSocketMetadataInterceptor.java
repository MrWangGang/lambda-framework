package org.lambda.framework.rsocket.adapter;

import io.netty.buffer.ByteBuf;
import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketStrategies;
import reactor.util.context.Context;

import static org.lambda.framework.common.enums.ConmonContract.AUTHTOKEN_STASH_NAMING;
import static org.lambda.framework.common.enums.ConmonContract.PRINCIPAL_STASH_NAMING;


public class RSocketMetadataInterceptor implements SocketAcceptorInterceptor {
    private MetadataExtractor metadataExtractor;
    public RSocketMetadataInterceptor(RSocketStrategies strategies) {
        this.metadataExtractor = strategies.metadataExtractor();
    }


    @Override
    public SocketAcceptor apply(SocketAcceptor socketAcceptor) {
        return (setup, sendingSocket) -> {
            ByteBuf byteBuf = setup.metadata();
            SecurityStash securityStash = getValueFromMetadata(byteBuf);
            String authToken = securityStash.getAuthToken();
            String principal = securityStash.getPrincipal();
            return socketAcceptor.accept(setup, sendingSocket)
                    .contextWrite(Context.of(AUTHTOKEN_STASH_NAMING,authToken))
                    .contextWrite(Context.of(AUTHTOKEN_STASH_NAMING,principal));
        };
    }

    private SecurityStash getValueFromMetadata(ByteBuf metadata) {
        SecurityStash securityStash = SecurityStash.builder().build();
        CompositeMetadata compositeMetadata = new CompositeMetadata(metadata, false);
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

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SecurityStash{
        private String authToken;

        private String principal;
    }


}
