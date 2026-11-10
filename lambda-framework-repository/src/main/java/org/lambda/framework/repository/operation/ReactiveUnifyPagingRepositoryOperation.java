package org.lambda.framework.repository.operation;

import org.lambda.framework.common.exception.EventException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.lambda.framework.enums.RepositoryExceptionEnum.INVALID_PAGING;
import static org.lambda.framework.enums.RepositoryExceptionEnum.PAGING_REQUIRED;

public interface ReactiveUnifyPagingRepositoryOperation<Entity> {
    default <C> Mono<Paged<Entity>> pageFrom(Paging paging, C condition, Sort sort,
            MappedPageQuery<C, Entity> source) {
        var request = request(paging, sort);
        return Mono.defer(() -> source.query(condition, request, request.getSort()))
                .map(page -> Paged.<Entity>builder().page(page.getNumber() + 1)
                        .size(page.getSize()).total(page.getTotalElements()).pages(page.getTotalPages())
                        .records(page.getContent()).build());
    }

    default <C,T> Mono<Paged<T>> queryPage(Paging paging, C condition, CountedPageQuery<C,T> source) {
        return queryPage(paging, condition, null, source);
    }

    default <C,T> Mono<Paged<T>> queryPage(Paging paging, C condition, Sort sort, CountedPageQuery<C,T> source) {
        return collect(paging, sort, () -> source.count(condition), request -> source.query(condition, request));
    }

    default <T> Mono<Paged<T>> queryPage(Paging paging, CountedPageSource<T> source) {
        return queryPage(paging, null, source);
    }

    default <T> Mono<Paged<T>> queryPage(Paging paging, Sort sort, CountedPageSource<T> source) {
        return collect(paging, sort, source::count, source::query);
    }

    private static <T> Mono<Paged<T>> collect(Paging paging, Sort sort, Supplier<Mono<Long>> count,
            Function<Pageable, Flux<T>> query) {
        var request = request(paging, sort);
        var total = Mono.defer(count).defaultIfEmpty(0L);
        var records = Flux.defer(() -> query.apply(request)).collectList();
        // A Mongo session cannot run parallel commands in the same transaction.
        return TransactionSynchronizationManager.forCurrentTransaction()
                .map(TransactionSynchronizationManager::isActualTransactionActive)
                .onErrorReturn(NoTransactionException.class, false)
                .flatMap(transaction -> transaction
                        ? total.flatMap(value -> records.map(items -> reactor.util.function.Tuples.of(value, items)))
                        : Mono.zip(total, records))
                .map(page -> Paged.<T>builder().page(paging.getPage()).size(paging.getSize())
                        .total(page.getT1()).pages((int) Math.ceil((double) page.getT1() / paging.getSize()))
                        .records(page.getT2()).build());
    }

    private static PageRequest request(Paging paging, Sort sort) {
        if (paging == null) {
            throw new EventException(PAGING_REQUIRED);
        }
        if (paging.getPage() == null || paging.getSize() == null || paging.getPage() < 1 || paging.getSize() < 1) {
            throw new EventException(INVALID_PAGING);
        }
        return PageRequest.of(paging.getPage() - 1, paging.getSize(), sort == null ? Sort.unsorted() : sort);
    }
}
