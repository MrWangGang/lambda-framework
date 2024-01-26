package org.lambda.framework.repository.operation.mysql;


import org.lambda.framework.repository.operation.ReactiveUnifyPagingRepositoryOperation;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface ReactiveMySqlCrudRepositoryOperation<Entity,IdType> extends ReactiveUnifyPagingRepositoryOperation, R2dbcRepository<Entity, IdType> {

}
