package org.lambda.framework.repository.operation.mangodb.listener;

import org.apache.commons.lang3.StringUtils;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.Document;
import org.lambda.framework.common.exception.EventException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.mongodb.client.model.changestream.OperationType.*;
import static org.lambda.framework.repository.enums.EnumOpertionType.*;
import static org.lambda.framework.enums.RepositoryExceptionEnum.*;

public class ReactiveMongoChangeStreamListener {
    private static final Logger log = LoggerFactory.getLogger(ReactiveMongoChangeStreamListener.class);

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public ReactiveMongoChangeStreamListener(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    public <T> void listener(ReactiveMongoChangeStreamOperate<T> operate) {
        if (operate == null || reactiveMongoTemplate == null || operate.clazz() == null) {
            throw new EventException(CHANGE_STREAM_CONFIGURATION_INVALID);
        }
        String docName = resolveCollectionName(operate.clazz());
        var resumeToken = new AtomicReference<BsonValue>();
        Flux.defer(() -> watch(operate.clazz(), docName, reactiveMongoTemplate, resumeToken.get()))
                .doOnNext(event -> {
                    if (event.getResumeToken() != null) {
                        resumeToken.set(event.getResumeToken());
                    }
                })
                .concatMap(event -> dispatch(operate, event)
                        .onErrorResume(error -> {
                            log.error("Mongo change stream event handling failed", error);
                            return Mono.empty();
                        }))
                .doOnError(error -> log.error("Mongo change stream disconnected, reconnecting", error))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(30)))
                .subscribe(ignored -> {}, error -> log.error("Mongo change stream terminated", error));
    }

    private <T> Mono<Void> dispatch(ReactiveMongoChangeStreamOperate<T> operate, ChangeStreamEvent<T> event) {
        var raw = event.getRaw();
        if (raw == null || raw.getClusterTime() == null || raw.getNamespaceDocument() == null
                || raw.getDocumentKey() == null || raw.getDocumentKey().get("_id") == null
                || event.getOperationType() == null) {
            return Mono.empty();
        }
        Long time = raw.getClusterTime().getValue();
        String db = raw.getNamespaceDocument().getString("db").getValue();
        String doc = raw.getNamespaceDocument().getString("coll").getValue();
        BsonValue idValue = raw.getDocumentKey().get("_id");
        BsonType type = idValue.getBsonType();
        String docId;
        if (type.equals(BsonType.STRING)) {
            docId = idValue.asString().getValue();
        } else if (type.equals(BsonType.OBJECT_ID)) {
            docId = idValue.asObjectId().getValue().toHexString();
        } else {
            return Mono.error(new EventException(CHANGE_STREAM_ID_TYPE_UNSUPPORTED));
        }
        String opid = db + "." + doc + "." + docId + "." + time;
        if (event.getOperationType().equals(INSERT)) {
            return operate.afterInsert(ENUM_OPERTION_TYPE_INSERT, opid, event.getBody()).then();
        }
        if (event.getOperationType().equals(UPDATE) || event.getOperationType().equals(REPLACE)) {
            return operate.afterUpdate(ENUM_OPERTION_TYPE_UPDATE, opid, event.getBody()).then();
        }
        if (event.getOperationType().equals(DELETE)) {
            return operate.afterDelete(ENUM_OPERTION_TYPE_DELETE, opid, event.getBody()).then();
        }
        return Mono.empty();
    }

    private static String resolveCollectionName(Class<?> clazz) {
        org.springframework.data.mongodb.core.mapping.Document annotation = clazz.getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class);
        if (annotation == null) {
            throw new EventException(COLLECTION_MAPPING_REQUIRED);
        }
        String docName = null;
        if (StringUtils.isNotBlank(annotation.value())) {
            docName = annotation.value();
        }
        if (StringUtils.isNotBlank(annotation.collection())) {
            docName = annotation.collection();
        }
        if (StringUtils.isBlank(docName)) {
            throw new EventException(COLLECTION_NAME_REQUIRED);
        }
        return docName;
    }

    private <T> Flux<ChangeStreamEvent<T>> watch(Class<T> clazz, String docName, ReactiveMongoTemplate reactiveMongoTemplate, BsonValue resumeToken) {
        ChangeStreamOptions.ChangeStreamOptionsBuilder builder = ChangeStreamOptions.builder()
                .filter(new Document("$match", new Document("operationType",
                        new Document("$in", List.of("update", "replace", "insert", "delete")))))
                .returnFullDocumentOnUpdate();
        if (resumeToken != null) {
            builder.resumeAfter(resumeToken);
        }
        return reactiveMongoTemplate.changeStream(docName, builder.build(), clazz);
    }
}
