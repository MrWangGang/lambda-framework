package org.lambda.framework.repository.operation;

import reactor.core.publisher.Mono;

import static java.lang.Math.ceil;

public interface ReactiveUnifyPagingRepositoryOperation {


    default <Condition,Entity>Mono<Paged<Entity>> find(Long page, Long size,Condition condition,UnifyPagingOperation<Entity> operation){
        return operation.count().switchIfEmpty(Mono.just(Long.valueOf(0))).flatMap(e->{
            return Mono.just(Paged.<Entity>builder().page(page).size(size).total(e).pages((long)(ceil(e/size))).build());
        }).flatMap(e->{
            return operation.<Entity>query().collectList().flatMap(records->{
                e.setRecords(records);
                return Mono.just(e);
            });
        });
    }
}
