package org.lambda.framework.repository.operation.mysql;


public interface ReactiveMySqlCrudRepositoryOperation<Entity,IdType> extends ReactiveMySqlCoreRepositoryOperation<Entity,Long>, ReactiveMySqlPagingRepositoryOperation<Entity> {

}
