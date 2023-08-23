package org.lambda.framework.repository.operation.mysql;


import org.lambda.framework.repository.operation.ReactiveUnifyPagingRepositoryOperation;

public interface ReactiveMySqlCrudRepositoryOperation<Entity,IdType> extends ReactiveMySqlCoreRepositoryOperation<Entity,IdType>, ReactiveUnifyPagingRepositoryOperation {

}
