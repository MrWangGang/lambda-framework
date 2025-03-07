package org.lambda.framework.compliance.service.impl;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.po.IFlattenTreePO;
import org.lambda.framework.common.po.UnifyPO;
import org.lambda.framework.compliance.service.IDefaultTreeService;
import org.lambda.framework.compliance.service.dto.*;
import org.lambda.framework.repository.operation.ReactiveUnifyPagingRepositoryOperation;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lambda.framework.compliance.enums.ComplianceConstant.ROOT_NODE_DEFAULT;
import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.*;


public class DefaultTreeServiceImpl<PO extends UnifyPO<ID> & IFlattenTreePO<ID>,ID,Repository extends ReactiveCrudRepository<PO,ID> & ReactiveSortingRepository<PO, ID> & ReactiveQueryByExampleExecutor<PO> & ReactiveUnifyPagingRepositoryOperation<PO>>  extends DefaultBasicServiceImpl<PO,ID,Repository> implements IDefaultTreeService<PO,ID> {

    private Class<PO> clazz;

    public DefaultTreeServiceImpl(Repository repository) {
        super(repository);
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
            if (typeArguments.length >= 1 && typeArguments[0] instanceof Class) {
                this.clazz = (Class<PO>) typeArguments[0];
            } else {
                throw new EventException(ES_COMPLIANCE_000,"获取节点类型失败");
            }
        } else {
            throw new EventException(ES_COMPLIANCE_000,"获取节点类型失败");
        }
    }


    //使用递归构建树
    //使用po参数是为了校验机构号这个必要的参数
    public Mono<List<PO>> findTree(FindTreeDTO<ID> dto) {
        if(dto == null || dto.getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_000,"节点所属机构ID不能为空");
        PO po = this.instance(clazz);
        po.setOrganizationId(dto.getOrganizationId());
        return super.find(po).collectList().flatMap(e->{
            return Mono.just(process(e, (ID) ROOT_NODE_DEFAULT));
        });
    }

    private List<PO> process(List<PO> flattenStream, ID parentId) {
        List<PO> tree = new ArrayList<>();
        flattenStream.forEach(po -> {
            if (Objects.equals(parentId, po.getParentId())) {
                po.setChildrens(process(flattenStream, po.getId()));
                tree.add(po);
            }
        });
        return tree;
    }


    public PO instance(Class<PO> clazz){
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new EventException(ES_COMPLIANCE_000,"实例化tree对象失败");
        }
    };


    /*
                    root
                A           B
            C       D   E       F
          将节点A移动到节点B上方：先将C  D的节点的parentId关联到root
          再将节点B的parentId关联到A
                    root
               C      D       A
                                   B
                                E       F



                2                     1                 1                   1                   1
            4       1       ->      2         ->>>  2        5     --->  2    5   6   --->  2   5  6
                  5   3          4   5  3       4   3   6               4  3                3
               6                   6                                                        4

          移动节点后，被移动的节点的子孙节点，只向上移动1级，为了保持结构尽量不变。

          以上测试全部通过

          全部有关节点查询都需要带上机构id
          全部的更新都是用批处理
         */
    public Mono<Void> moveNode(MoveNodeDTO<ID> dto) {
        if(dto.getTargetNodeId() == null)throw new EventException(ES_COMPLIANCE_000,"目标节点传入值不符合规范 targetNodeId < -1");
        if(dto.getCurrentNodeId() == null)throw new EventException(ES_COMPLIANCE_000,"当前节点不能为空");
        if(dto.getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_000,"节点所属机构ID不能为空");
        PO _currentNode =  instance(clazz);
        _currentNode.setId(dto.getCurrentNodeId());
        _currentNode.setOrganizationId(dto.getOrganizationId());
        //校验
        return super.get(_currentNode)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_000,"当前节点不存在")))
                .flatMap(e -> {
                    if (e.getParentId().equals(ROOT_NODE_DEFAULT)) {
                        return Mono.error(new EventException(ES_COMPLIANCE_000,"不能移动根节点"));
                    }
                    return Mono.just(e);
                })
                .flatMap(e -> {
                    //代表着，将当前节点变为root节点
                    if (dto.getTargetNodeId().equals(ROOT_NODE_DEFAULT)) {
                        PO _current =  instance(clazz);
                        _current.setId(e.getParentId());
                        _current.setOrganizationId(dto.getOrganizationId());
                        return super.find(_current).flatMap(root -> {
                            //先记录之前的parentId,用来改变原先的子节点的挂靠
                            ID oldParentId =  e.getParentId();
                            //将当前节点变成根节点
                            e.setParentId((ID) ROOT_NODE_DEFAULT);
                            //查出当前变动节点的子孙节点
                            PO _children = instance(clazz);
                            _children.setParentId(e.getId());
                            _children.setOrganizationId(dto.getOrganizationId());
                            Flux<PO> currentChildren = super.find(_children)
                                    .flatMap(x->{
                                        x.setParentId((ID) oldParentId);
                                        return Flux.just(x);
                                    });
                            Flux<PO> currentChildrenUpdate = super.update(currentChildren);
                            Mono<Void> currentSelfUpdate = super.update(e).then();
                            root.setParentId(e.getId());
                            Mono<Void> rootSelfUpdate =  super.update(root).then();
                            //concat用于顺序执行流，不会造成脏读
                            return Flux.concat(currentChildrenUpdate, rootSelfUpdate, currentSelfUpdate).then();
                        }).then();
                    }
                    PO _target =  instance(clazz);
                    _target.setId(dto.getTargetNodeId());
                    _target.setOrganizationId(dto.getOrganizationId());
                    return super.get(_target)
                            .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_000,"目标节点不存在")))
                            .then(Mono.just(e)).flatMap(x->{
                                ID oldParentId = x.getParentId();
                                x.setParentId(dto.getTargetNodeId());
                                Mono<Void> currentSelfUpdate = super.update(x).then();
                                PO  _children = instance(clazz);
                                _children.setParentId(x.getId());
                                _children.setOrganizationId(dto.getOrganizationId());
                                Flux<PO> currentChildren = super.find(_children)
                                        .flatMap(y->{
                                            y.setParentId(oldParentId);
                                            return Flux.just(y);
                                        });
                                Mono<Void> currentChildrenUpdate = super.update(currentChildren).then();
                                return Flux.concat(currentChildrenUpdate,currentSelfUpdate).then();
                            });
                });
    }

    @Override
    public Mono<Void> buildRoot(BuildRootDTO<PO,ID> dto) {
        if(dto == null || dto.getNode() == null)throw new EventException(ES_COMPLIANCE_000,"创建节点时,节点信息必须存在");
        if(dto.getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_000,"节点所属机构ID不能为空");
        //要创建一个根节点,先检查之前有无根节点
        PO po = instance(clazz);
        po.setOrganizationId(dto.getOrganizationId());
        //有数据则表示已经存在根节点了，不能创建根节点
        return super.find(po).hasElements().flatMap(e->{
                    if(e)return Mono.error(new EventException(ES_COMPLIANCE_000,"此机构已经创建了树模型，无法创建根"));
                    //设置项
                    dto.getNode().setParentId((ID) ROOT_NODE_DEFAULT);
                    dto.getNode().setOrganizationId(dto.getOrganizationId());
                    return super.insert(dto.getNode());
                }).then();
    }

    @Override
    public Mono<Void> buildNode(BuildNodeDTO<PO,ID> dto) {
        if(dto == null || dto.getNode() == null)throw new EventException(ES_COMPLIANCE_000,"创建节点时,节点信息必须存在");
        if(dto.getTargetNodeId() == null)throw new EventException(ES_COMPLIANCE_000,"目标节点传入值不符合规范 targetNodeId < -1");
        if(dto.getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_000,"节点所属机构ID不能为空");
        //先检查 targetNodeId 代表的节点有无存在
        PO po = instance(clazz);
        po.setId(dto.getTargetNodeId());
        po.setOrganizationId(dto.getOrganizationId());
        return super.find(po)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_000,"目标节点不存在")))
                .flatMap(e->{
                    //设置目标节点为父节点
                    dto.getNode().setParentId(e.getId());
                    dto.getNode().setOrganizationId(e.getOrganizationId());
                    return super.insert(dto.getNode());
                }).then();
    }

    @Override
    public Mono<Void> editNode(EditNodeDTO<PO,ID> dto) {
        if(dto == null || dto.getNode() == null)throw new EventException(ES_COMPLIANCE_000,"创建节点时,节点信息必须存在");
        if(dto.getTargetNodeId() == null)throw new EventException(ES_COMPLIANCE_000,"目标节点传入值不符合规范 targetNodeId < -1");
        if(dto.getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_000,"节点所属机构ID不能为空");
        PO po = instance(clazz);
        po.setId(dto.getTargetNodeId());
        po.setOrganizationId(dto.getOrganizationId());
        //先检查当前节点是否存在
        return super.find(po)
                 //不存在则抛出异常
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_000,"目标节点不存在")))
                .flatMap(e->{
                    //存在则更新
                    //组装3要素
                    dto.getNode().setId(e.getId());
                    dto.getNode().setOrganizationId(e.getOrganizationId());
                    dto.getNode().setParentId(e.getParentId());
                    //将统一的信息复制过去
                    dto.getNode().setCreateTime(e.getCreateTime());
                    dto.getNode().setUpdateTime(e.getUpdateTime());

                    return super.update(dto.getNode());
                }).then();
    }
    @Override
    public Mono<Void> removeNode(RemoveNodeDTO<ID> dto) {
        if(dto == null)throw new EventException(ES_COMPLIANCE_000,"创建节点时,节点信息必须存在");
        if(dto.getTargetNodeId() == null)throw new EventException(ES_COMPLIANCE_000,"目标节点传入值不符合规范 targetNodeId < -1");
        if(dto.getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_000,"节点所属机构ID不能为空");
        //先判断是不是根节点，根节点不允许删除
        PO po = instance(clazz);
        po.setId(dto.getTargetNodeId());
        po.setOrganizationId(dto.getOrganizationId());
        return super.find(po)
                //不存在则抛出异常
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_000,"目标节点不存在")))
                .flatMap(e->{
                    //存在则判断是否为根节点
                    if(ROOT_NODE_DEFAULT.equals(e.getParentId())){
                     //根节点要先判断他有无子节点，才能删除
                        PO _rootChildren = instance(clazz);
                        _rootChildren.setParentId(e.getId());
                        _rootChildren.setOrganizationId(e.getOrganizationId());
                        return super.find(_rootChildren).hasElements().flatMap(hasChildren->{
                            //有子节点，不让删除
                            if(hasChildren)return Mono.error(new EventException(ES_COMPLIANCE_000,"此根节点拥有子树，不允许删除"));
                            //没有可以删除
                            PO _rootDelete = instance(clazz);
                            _rootDelete.setId(e.getId());
                            _rootDelete.setOrganizationId(e.getOrganizationId());
                            return super.deleteBy(_rootDelete);
                        });
                    }
                    PO _childrenDelete = instance(clazz);
                    _childrenDelete.setId(e.getId());
                    _childrenDelete.setOrganizationId(e.getOrganizationId());
                    Mono<Void> deleteSelf = super.deleteBy(_childrenDelete);

                    //删除当前节点,并将他们的子节点向前移动，挂靠在之前的parentId上
                    PO _childrenUpdate = instance(clazz);
                    _childrenUpdate.setParentId(e.getId());
                    _childrenUpdate.setOrganizationId(e.getOrganizationId());
                    Flux<PO> childrenUpdate =  super.find(_childrenUpdate).flatMap(x->{
                        x.setParentId(e.getParentId());
                        return Flux.just(x);
                    });
                    Flux<PO> update = super.update(childrenUpdate);
                    return Flux.concat(update,deleteSelf).then();
                }).then();
    }
}
