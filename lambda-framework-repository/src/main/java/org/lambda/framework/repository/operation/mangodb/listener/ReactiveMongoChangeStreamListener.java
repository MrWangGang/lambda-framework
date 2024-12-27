package org.lambda.framework.repository.operation.mangodb.listener;

import org.apache.commons.lang3.StringUtils;
import org.bson.BsonType;
import org.bson.Document;
import org.lambda.framework.common.exception.EventException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

import java.util.List;

import static com.mongodb.client.model.changestream.OperationType.*;
import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public class ReactiveMongoChangeStreamListener {
    private static final Logger log = LoggerFactory.getLogger(ReactiveMongoChangeStreamListener.class);

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public ReactiveMongoChangeStreamListener(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    public <T>void listener(ReactiveMongoChangeStreamOperate<T> operate) {
        try {
            this.start(operate);
        } catch (Throwable e){
            log.error(StringUtils.isNotBlank(e.getMessage())?e.getMessage():"change stream配置错误,启动失败", e);
            System.exit(1);
        }
    }

    private <T> void start(ReactiveMongoChangeStreamOperate<T> operate) {
        if (operate == null) throw new EventException(ES_REPOSITORY_MONGO_016);
        if (reactiveMongoTemplate == null) throw new EventException(ES_REPOSITORY_MONGO_016);
        if (operate.clazz() == null) throw new EventException(ES_REPOSITORY_MONGO_016);
        this.watch(operate.clazz(), reactiveMongoTemplate)
                .subscribe(event -> {
                    try {
                        Long time = event.getRaw().getClusterTime().getValue();
                        String db = event.getRaw().getNamespaceDocument().getString("db").getValue();
                        String doc = event.getRaw().getNamespaceDocument().getString("coll").getValue();
                        BsonType type = event.getRaw().getDocumentKey().get("_id").getBsonType();
                        String docId;
                        if (type.equals(BsonType.STRING)) {
                            // 如果是字符串类型，使用getString方法获取值
                            docId = event.getRaw().getDocumentKey().get("_id").asString().getValue();
                            // 后续使用idAsString进行相应操作，比如记录日志、作为参数传递等
                        } else if (type.equals(BsonType.OBJECT_ID)) {
                            // 如果是ObjectId类型，使用getObjectId方法获取ObjectId对象后再转换为字符串（通常需要这样处理以便后续使用）
                            docId = event.getRaw().getDocumentKey().get("_id").asObjectId().getValue().toHexString();
                            // 后续使用idAsString进行操作
                        } else {
                            // 处理其他不符合预期的类型情况，比如抛出异常或者进行默认处理等
                            throw new EventException(ES_REPOSITORY_MONGO_017);
                        }
                        String opid = db + "." + doc + "." + docId + "." + time;
                        if (event.getOperationType().equals(INSERT)) {
                            operate.afterInsert(opid, event.getBody()).subscribe();
                        }
                        if (event.getOperationType().equals(UPDATE)) {
                            operate.afterUpdate(opid, event.getBody()).subscribe();
                        }
                        if (event.getOperationType().equals(DELETE)) {
                            operate.afterDelete(opid, event.getBody()).subscribe();
                        }
                    } catch (Throwable e){
                        log.error(StringUtils.isNotBlank(e.getMessage())?e.getMessage():"change stream配置错误,启动失败", e);
                    }
                });
    }


    private  <T>Flux<ChangeStreamEvent<T>> watch(Class<T> clazz,ReactiveMongoTemplate reactiveMongoTemplate) {
        org.springframework.data.mongodb.core.mapping.Document annotation = clazz.getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class);
        if(annotation == null){
            throw new EventException(ES_REPOSITORY_MONGO_018);
        }
        String docName = null;
        if(StringUtils.isNotBlank(annotation.value())){
            docName = annotation.value();
        }
        if(StringUtils.isNotBlank(annotation.collection())){
            docName = annotation.collection();
        }

        if(StringUtils.isBlank(docName)){
            throw new EventException(ES_REPOSITORY_MONGO_019);
        }
        // 构建 ChangeStreamOptions，过滤操作类型为 insert 和 update,delete
        ChangeStreamOptions options = ChangeStreamOptions.builder()
                .filter(new Document("$match", new Document("operationType",
                        new Document("$in", List.of("insert", "update", "delete"))))) // 构建过滤条件
                .returnFullDocumentOnUpdate() // 对于更新操作返回完整文档
                .build();
        return reactiveMongoTemplate.changeStream(docName,options, clazz);
    }
}
