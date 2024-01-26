package org.lambda.framework.repository.operation.mangodb;


import org.lambda.framework.repository.operation.ReactiveUnifyPagingRepositoryOperation;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface ReactiveMongoCrudRepositoryOperation<Entity,IdType> extends ReactiveUnifyPagingRepositoryOperation, R2dbcRepository<Entity, IdType> {

}
