package org.lambda.framework.repository.operation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.lang.Math.ceil;

public interface ReactiveUnifyPagingRepositoryOperation<Entity> {

    public interface Process<Entity> {
        public Mono<Integer> count();

        public Flux<Entity> query();
    }

    default Mono<Paged<Entity>> find(Integer page, Integer size,Process<Entity> process){
        return process.count().switchIfEmpty(Mono.just(Integer.valueOf(0))).flatMap(e->{
            return Mono.just(Paged.<Entity>builder().page(page).size(size).total(e).pages(((int)ceil(e/size))).build());
        }).flatMap(e->{
            return process.query().collectList().flatMap(records->{
                e.setRecords(records);
                return Mono.just(e);
            });
        });
    }
}
