package org.lambda.framework.repository.operation.qdrant;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.CollectionOperationResponse;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.UpdateResult;
import io.qdrant.client.grpc.Points.UpsertPoints;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.util.ReactiveValues;
import org.lambda.framework.enums.QdrantExceptionEnum;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ReactiveQdrantOperations implements AutoCloseable {
    private final List<QdrantClient> clients;
    private final AtomicInteger cursor = new AtomicInteger();

    public ReactiveQdrantOperations(List<QdrantClient> clients) {
        if (clients.isEmpty()) {
            throw new IllegalArgumentException("At least one Qdrant client is required");
        }
        this.clients = List.copyOf(clients);
    }

    public Flux<ScoredPoint> query(QueryPoints query) {
        return execute(client -> client.queryAsync(query)).flatMapIterable(Function.identity());
    }

    public Mono<UpdateResult> upsert(UpsertPoints points) {
        return ReactiveValues.require(execute(client -> client.upsertAsync(points)));
    }

    public Mono<Boolean> collectionExists(String name) {
        return ReactiveValues.require(execute(client -> client.collectionExistsAsync(name)));
    }

    public Mono<CollectionOperationResponse> createCollection(CreateCollection collection) {
        return ReactiveValues.require(execute(client -> client.createCollectionAsync(collection)))
                .flatMap(result -> awaitCollection(collection.getCollectionName()).thenReturn(result));
    }

    public Mono<Void> awaitCollection(String name) {
        return Flux.fromIterable(clients).flatMap(client -> ReactiveValues.require(fromFuture(() -> client.collectionExistsAsync(name)))
                .flatMap(exists -> exists ? Mono.<Void>empty()
                        : Mono.error(new EventException(QdrantExceptionEnum.COLLECTION_NOT_READY,
                                "Qdrant collection is not visible on every node")))
                .retryWhen(Retry.fixedDelay(12, Duration.ofMillis(250)))).then();
    }

    public Mono<CollectionOperationResponse> deleteCollection(String name) {
        return ReactiveValues.require(execute(client -> client.deleteCollectionAsync(name)));
    }

    public Mono<Void> check() {
        return Flux.fromIterable(clients).flatMap(client -> ReactiveValues.require(fromFuture(client::healthCheckAsync))).then();
    }

    private <T> Mono<T> execute(Function<QdrantClient, ListenableFuture<T>> call) {
        return fromFuture(() -> call.apply(clients.get(Math.floorMod(cursor.getAndIncrement(), clients.size()))));
    }

    private static <T> Mono<T> fromFuture(Supplier<ListenableFuture<T>> request) {
        return Mono.create(sink -> {
            var future = request.get();
            sink.onCancel(() -> future.cancel(true));
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(T value) {
                    sink.success(value);
                }

                @Override
                public void onFailure(Throwable error) {
                    sink.error(error);
                }
            }, MoreExecutors.directExecutor());
        });
    }

    @Override
    public void close() {
        clients.forEach(QdrantClient::close);
    }
}
