package org.lambda.framework.repository.operation.mysql;


import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface ReactiveMySqlCrudRepositoryOperation<Entity,IdType> extends R2dbcRepository<Entity, IdType> {

}
