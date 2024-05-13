package org.lambda.framework.repository.operation.mangodb;


import org.lambda.framework.repository.operation.ReactiveUnifyPagingRepositoryOperation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ReactiveMongoCrudRepositoryOperation<Entity,IdType> extends ReactiveUnifyPagingRepositoryOperation<Entity>, ReactiveMongoRepository<Entity, IdType> {

}
