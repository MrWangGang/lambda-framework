package org.lambda.framework.repository.operation.qdrant;

import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.UpdateResult;
import io.qdrant.client.grpc.Points.UpsertPoints;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;

/** Binds a business repository to one collection and maps its search results. */
public abstract class ReactiveQdrantRepository<T> {
    private final ReactiveQdrantOperations operations;
    private final String collection;
    private final Function<ScoredPoint, T> mapper;

    protected ReactiveQdrantRepository(ReactiveQdrantOperations operations, String collection,
                                       Function<ScoredPoint, T> mapper) {
        this.operations = Objects.requireNonNull(operations);
        this.collection = Objects.requireNonNull(collection);
        this.mapper = Objects.requireNonNull(mapper);
        if (collection.isBlank()) {
            throw new IllegalArgumentException("Qdrant collection name must not be blank");
        }
    }

    protected final Mono<Void> ensureCollection(CreateCollection.Builder definition) {
        var request = definition.setCollectionName(collection).build();
        return operations.collectionExists(collection).flatMap(exists -> {
            if (exists) {
                return operations.awaitCollection(collection);
            }
            return operations.createCollection(request).then()
                    .onErrorResume(error -> operations.collectionExists(collection).flatMap(created ->
                            created ? operations.awaitCollection(collection) : Mono.error(error)));
        });
    }

    protected final Mono<UpdateResult> upsert(UpsertPoints.Builder points) {
        return operations.upsert(points.setCollectionName(collection).build());
    }

    protected final Flux<T> query(QueryPoints.Builder query) {
        return operations.query(query.setCollectionName(collection).build()).map(mapper);
    }
}
