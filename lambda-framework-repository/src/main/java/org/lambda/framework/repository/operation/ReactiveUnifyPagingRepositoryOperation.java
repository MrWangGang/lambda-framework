package org.lambda.framework.repository.operation;

import reactor.core.publisher.Mono;

import static java.lang.Math.ceil;

public interface ReactiveUnifyPagingRepositoryOperation<Entity> {


    default Mono<Paged<Entity>> find(Integer page, Integer size,UnifyPagingOperation<Entity> operation){
        return operation.count().switchIfEmpty(Mono.just(Integer.valueOf(0))).flatMap(e->{
            return Mono.just(Paged.<Entity>builder().page(page).size(size).total(e).pages(((int)ceil(e/size))).build());
        }).flatMap(e->{
            return operation.query().collectList().flatMap(records->{
                e.setRecords(records);
                return Mono.just(e);
            });
        });
    }
}
