package org.lambda.framework.repository.operation.mysql;


public interface ReactiveMySqlCrudRepositoryOperation<Entity,IdType> extends ReactiveMySqlCoreRepositoryOperation<Entity,IdType>, ReactiveMySqlPagingRepositoryOperation<Entity> {

}
