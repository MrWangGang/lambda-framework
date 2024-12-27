package org.lambda.framework.repository.operation.elasticsearch;


import org.lambda.framework.repository.operation.ReactiveUnifyPagingRepositoryOperation;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;

public interface ReactiveElasticsearchCrudRepositoryOperation<Entity,IdType> extends ReactiveUnifyPagingRepositoryOperation<Entity>, ReactiveElasticsearchRepository<Entity, IdType> {

}
