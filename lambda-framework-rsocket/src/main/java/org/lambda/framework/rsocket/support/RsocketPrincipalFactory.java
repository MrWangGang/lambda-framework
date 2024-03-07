package org.lambda.framework.rsocket.support;

import io.rsocket.metadata.CompositeMetadata;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.security.PrincipalFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.lambda.framework.common.enums.ConmonContract.*;
import static org.lambda.framework.rsocket.enums.RsocketExceptionEnum.ES_RSOCKET_002;

@Component
public class RsocketPrincipalFactory extends PrincipalFactory {

    @Override
    public Mono<String> getAuthToken(){
        return getRequest(AUTHTOKEN_STASH_NAMING);
    }

    @Override
    protected Mono<String> fetchSubject() {
        return getRequest(PRINCIPAL_STASH_NAMING);
    }

    private Mono<String> getRequest(String key) {
        return Mono.deferContextual(Mono::just)
                .map(contextView -> {
                    Object o = contextView.get(PRINCIPAL_STASH_NAMING);
                    return o.toString();
                });  // 使用 RSocketRequester 获取元数据
    }

    private static String getMetadataValue(CompositeMetadata compositeMetadata, String mimeType) {
        // 使用流遍历 CompositeMetadata 中的元数据块
        return compositeMetadata.stream()
                .filter(entry -> entry.getMimeType().equals(mimeType))
                .findFirst()
                .map(entry -> entry.getContent().toString()).orElseThrow(()->new EventException(ES_RSOCKET_002));
    }
}
