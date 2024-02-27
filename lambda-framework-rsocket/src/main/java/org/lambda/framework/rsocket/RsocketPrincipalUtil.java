package org.lambda.framework.rsocket;

import io.rsocket.Payload;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.WellKnownMimeType;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.security.PrincipalFactory;
import org.lambda.framework.compliance.security.container.SecurityContract;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import reactor.core.publisher.Mono;

import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.ES_COMPLIANCE_021;
import static org.lambda.framework.compliance.security.container.SecurityContract.PRINCIPAL_STASH_NAMING;
import static org.lambda.framework.rsocket.enums.RsocketExceptionEnum.ES_RSOCKET_000;

public abstract class RsocketPrincipalUtil extends PrincipalFactory {
    public RsocketPrincipalUtil(ReactiveRedisOperation reactiveRedisOperation) {
        super(reactiveRedisOperation);
    }

    @Override
    protected Mono<String> getAuthToken(){
        return getRequest(SecurityContract.AUTH_TOKEN_NAMING);
    }

    @Override
    protected Mono<String> fetchPrincipal() {
        return getRequest(PRINCIPAL_STASH_NAMING);
    }
    private Mono<String> getRequest(String key) {
        return Mono.deferContextual(Mono::just)
                .map(contextView -> contextView.get(Payload.class))  // 使用 RSocketRequester 获取元数据
                .switchIfEmpty(Mono.error(new EventException(ES_RSOCKET_000)))
                .flatMap(payload -> {
                    CompositeMetadata compositeMetadata = new CompositeMetadata(payload.metadata(), false);
                    // 获取特定类型的元数据块
                    String metadata = getMetadataValue(compositeMetadata, WellKnownMimeType.APPLICATION_JSON.getString());
                    return Mono.just(metadata);
                })
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_021)));
    }

    private static String getMetadataValue(CompositeMetadata compositeMetadata, String mimeType) {
        // 使用流遍历 CompositeMetadata 中的元数据块
        return compositeMetadata.stream()
                .filter(entry -> entry.getMimeType().equals(mimeType))
                .findFirst()
                .map(entry -> entry.getContent().toString()).orElseThrow(()->new EventException(ES_COMPLIANCE_021));
    }
}