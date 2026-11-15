package org.lambda.framework.repository.operation;

import com.mongodb.MongoCommandException;
import com.mongodb.reactivestreams.client.MongoDatabase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class MongoDocumentSchemas {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDocumentSchemas.class);
    private static final int NAMESPACE_NOT_FOUND = 26;
    private static final int NAMESPACE_ALREADY_EXISTS = 48;

    private MongoDocumentSchemas() {}

    public static Mono<Void> install(ReactiveMongoTemplate mongo, Resource resource) {
        return install(mongo, resource, (database, collection) -> Mono.empty());
    }

    public static Mono<Void> install(ReactiveMongoTemplate mongo, Resource resource,
                                     BiFunction<MongoDatabase, String, Mono<Void>> migrate) {
        return Mono.fromCallable(() -> Document.parse(resource.getContentAsString(StandardCharsets.UTF_8)))
                .flatMap(schemas -> mongo.getMongoDatabase().flatMap(database ->
                        Flux.fromIterable(schemas.entrySet())
                                .concatMap(entry -> install(database, entry.getKey(), (Document) entry.getValue(), migrate))
                                .then(Mono.fromRunnable(() -> LOG.info("MongoDB {}: {} document collection validators installed",
                                        database.getName(), schemas.size())))));
    }

    private static Mono<Void> install(MongoDatabase database, String collection, Document schema,
                                      BiFunction<MongoDatabase, String, Mono<Void>> migrate) {
        var required = new ArrayList<>(schema.getList("required", String.class));
        required.addAll(List.of("_id", "createdAt", "updatedAt", "version"));
        schema.put("required", required);
        schema.get("properties", Document.class)
                .append("_id", new Document("bsonType", "objectId"))
                .append("createdAt", new Document("bsonType", "date"))
                .append("updatedAt", new Document("bsonType", "date"))
                .append("version", new Document("bsonType", List.of("int", "long")).append("minimum", 0));
        var validator = new Document("$jsonSchema", schema);
        var modify = command("collMod", collection, validator);
        return Mono.from(database.runCommand(modify))
                .onErrorResume(MongoCommandException.class, failure -> {
                    if (failure.getErrorCode() != NAMESPACE_NOT_FOUND) return Mono.error(failure);
                    return Mono.from(database.runCommand(command("create", collection, validator)))
                            .onErrorResume(MongoCommandException.class, createFailure ->
                                    createFailure.getErrorCode() == NAMESPACE_ALREADY_EXISTS
                                            ? Mono.from(database.runCommand(modify)) : Mono.error(createFailure));
                })
                .then(Mono.defer(() -> migrate.apply(database, collection)))
                .then(normalizeBlankText(database, collection, schema.get("properties", Document.class)))
                .then(Mono.from(database.getCollection(collection)
                        .countDocuments(new Document("$nor", List.of(validator)))))
                .doOnNext(count -> {
                    if (count > 0) LOG.warn("MongoDB {}.{} has {} existing documents outside its write schema",
                            database.getName(), collection, count);
                }).then();
    }

    private static Mono<Void> normalizeBlankText(MongoDatabase database, String collection, Document properties) {
        var values = new Document();
        var blankFields = new ArrayList<Document>();
        properties.forEach((field, value) -> {
            var type = ((Document) value).get("bsonType");
            if (!List.of("string", "null").equals(type)) return;
            var blank = new Document("$regex", "^\\s*$");
            blankFields.add(new Document(field, blank));
            var reference = "$" + field;
            var isText = new Document("$eq", List.of(new Document("$type", reference), "string"));
            var input = new Document("$cond", List.of(isText, reference, "value"));
            var isBlank = new Document("$regexMatch", new Document("input", input).append("regex", "^\\s*$"));
            values.put(field, new Document("$cond", java.util.Arrays.asList(isBlank, null, reference)));
        });
        if (blankFields.isEmpty()) return Mono.empty();
        return Mono.from(database.getCollection(collection).updateMany(
                new Document("$or", blankFields), List.of(new Document("$set", values)))).then();
    }

    private static Document command(String operation, String collection, Document validator) {
        return new Document(operation, collection).append("validator", validator)
                .append("validationLevel", "strict").append("validationAction", "error");
    }
}
