package org.lambda.framework.repository.operation;

import org.lambda.framework.common.exception.EventException;
import reactor.core.publisher.Mono;

import static java.lang.Math.ceil;
import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.ES_REPOSITORY_MYSQL_100;

public interface ReactiveUnifyPagingRepositoryOperation {


    default <Condition,Entity>Mono<Paged<Entity>> find(Long page, Long size,Condition condition,UnifyPagingOperation<Entity> operation){
        if(page == null || page<0  || size == null || size <0 || size == 0)throw new EventException(ES_REPOSITORY_MYSQL_100);
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
