package org.lambda.framework.compliance.service.impl;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.repository.po.IFlattenTreePO;
import org.lambda.framework.compliance.repository.po.UnifyPO;
import org.lambda.framework.compliance.service.IDefaultTreeService;
import org.lambda.framework.compliance.service.dto.*;
import org.lambda.framework.repository.operation.mysql.ReactiveMySqlCrudRepositoryOperation;
import org.lambda.framework.security.SecurityPrincipalUtil;
import org.springframework.data.domain.Example;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.lambda.framework.compliance.enums.ComplianceConstant.ROOT_NODE_DEFAULT;
import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.*;


public class DefaultTreeServiceImpl<PO extends UnifyPO & IFlattenTreePO,ID,Repository extends ReactiveMySqlCrudRepositoryOperation<PO,ID>>  extends DefaultBasicServiceImpl<PO,ID,Repository> implements IDefaultTreeService<PO,ID> {

    @Resource
    private SecurityPrincipalUtil securityPrincipalUtil;

    public DefaultTreeServiceImpl(Repository repository) {
        super(repository);
    }

    //使用递归构建树
    //使用po参数是为了校验机构号这个必要的参数
    public Mono<List<PO>> findTree(Class<PO> clazz, FindTreeDTO dto) {
        if(dto.getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_013);
        PO po = this.instance(clazz);
        po.setOrganizationId(dto.getOrganizationId());
        Example<PO> example = Example.of(po);
        return repository.findAll(example).collectList().flatMap(e->{
            return Mono.just(process(e, ROOT_NODE_DEFAULT));
        });
    }

    private List<PO> process(List<PO> flattenStream, Long parentId) {
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
        } catch (InstantiationException e) {
            throw new EventException(ES_COMPLIANCE_014);
        } catch (IllegalAccessException e) {
            throw new EventException(ES_COMPLIANCE_014);
        } catch (InvocationTargetException e) {
            throw new EventException(ES_COMPLIANCE_014);
        } catch (NoSuchMethodException e) {
            throw new EventException(ES_COMPLIANCE_014);
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
    public Mono<Void> moveNode(Class<PO> clazz, MoveNodeDTO dto) {
        if(dto.getTargetNodeId() == null || dto.getTargetNodeId().longValue() < ROOT_NODE_DEFAULT)throw new EventException(ES_COMPLIANCE_010);
        if(dto.getCurrentNodeId() == null)throw new EventException(ES_COMPLIANCE_011);
        if(dto.getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_013);
        PO _currentNode =  instance(clazz);
        _currentNode.setId(dto.getCurrentNodeId());
        _currentNode.setOrganizationId(dto.getOrganizationId());
        //校验
        return super.get(_currentNode)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_006)))
                .flatMap(e -> {
                    if (e.getParentId().equals(ROOT_NODE_DEFAULT)) {
                        return Mono.error(new EventException(ES_COMPLIANCE_008));
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
                            Long oldParentId = e.getParentId();
                            //将当前节点变成根节点
                            e.setParentId(ROOT_NODE_DEFAULT);
                            //查出当前变动节点的子孙节点
                            PO _children = instance(clazz);
                            _children.setParentId(e.getId());
                            _children.setOrganizationId(dto.getOrganizationId());
                            Flux<PO> currentChildren = super.find(_children)
                                    .flatMap(x->{
                                        x.setParentId(oldParentId);
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
                            .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_007)))
                            .then(Mono.just(e)).flatMap(x->{
                                Long oldParentId = x.getParentId();
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
    public Mono<Void> buildRoot(Class<PO> clazz, BuildRootDTO<PO> dto) {
        if(dto.getNode() == null)throw new EventException(ES_COMPLIANCE_001);
        if(dto.getNode().getId() != null)throw new EventException(ES_COMPLIANCE_002);
        if(dto.getNode().getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_013);
        //要创建一个根节点,先检查之前有无根节点
        PO po = instance(clazz);
        po.setOrganizationId(dto.getNode().getOrganizationId());
        Example<PO> _nodeByOrgId = Example.of(po);
        //有数据则表示已经存在根节点了，不能创建根节点
        return repository.findAll(_nodeByOrgId).hasElements().flatMap(e->{
                    if(!e)return Mono.error(new EventException(ES_COMPLIANCE_003));
                    dto.getNode().setParentId(ROOT_NODE_DEFAULT);
                    return repository.save(dto.getNode());
                }).then();
    }

    @Override
    public Mono<Void> buildNode(Class<PO> clazz,BuildNodeDTO<PO> dto) {
        if(dto.getTargetNodeId() == null || dto.getTargetNodeId().longValue() < ROOT_NODE_DEFAULT)throw new EventException(ES_COMPLIANCE_010);
        if(dto.getNode() == null)throw new EventException(ES_COMPLIANCE_001);
        if(dto.getNode().getId() != null)throw new EventException(ES_COMPLIANCE_002);
        if(dto.getNode().getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_013);
        //先检查 targetNodeId 代表的节点有无存在
        PO po = instance(clazz);
        po.setId(dto.getTargetNodeId());
        po.setOrganizationId(dto.getNode().getOrganizationId());
        Example<PO> _targetNode = Example.of(po);
        return repository.findOne(_targetNode)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_007)))
                .flatMap(e->{
                    PO _buildNodeEntity = dto.getNode();
                    //设置目标节点为父节点
                    _buildNodeEntity.setParentId(e.getId());
                    return repository.save(_buildNodeEntity);
                }).then();
    }

    @Override
    public Mono<Void> editNode(Class<PO> clazz,EditNodeDTO<PO> dto) {
        if(dto.getNode() == null)throw new EventException(ES_COMPLIANCE_001);
        if(dto.getNode().getId() == null)throw new EventException(ES_COMPLIANCE_015);
        if(dto.getNode().getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_013);
        PO po = instance(clazz);
        po.setId(dto.getNode().getId());
        po.setOrganizationId(dto.getNode().getOrganizationId());
        Example<PO> _targetNode = Example.of(po);
        //先检查当前节点是否存在
        return repository.findOne(_targetNode)
                 //不存在则抛出异常
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_007)))
                .flatMap(e->{
                    //存在则更新
                    return repository.save(e);
                }).then();
    }
    @Override
    public Mono<Void> removeNode(Class<PO> clazz,RemoveNodeDTO dto) {
        if(dto == null)throw new EventException(ES_COMPLIANCE_001);
        if(dto.getTargetNodeId() == null || dto.getTargetNodeId().longValue() < ROOT_NODE_DEFAULT)throw new EventException(ES_COMPLIANCE_010);
        if(dto.getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_013);
        //先判断是不是根节点，根节点不允许删除
        PO po = instance(clazz);
        po.setId(dto.getTargetNodeId());
        po.setOrganizationId(dto.getOrganizationId());
        Example<PO> _targetNode = Example.of(po);
        return repository.findOne(_targetNode)
                //不存在则抛出异常
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_007)))
                .flatMap(e->{
                    //存在则判断是否为根节点
                    if(ROOT_NODE_DEFAULT.equals(e.getParentId())){
                     //根节点不允许删除
                        return Mono.error(new EventException(ES_COMPLIANCE_016));
                    }
                    PO _childrenDelete = instance(clazz);
                    _childrenDelete.setId(e.getId());
                    _childrenDelete.setOrganizationId(dto.getOrganizationId());
                    Iterable<? extends PO> childDeleteIterable = Collections.singletonList(_childrenDelete);
                    Mono<Void> deleteSelf = super.delete(childDeleteIterable);

                    //删除当前节点,并将他们的子节点向前移动，挂靠在之前的parentId上
                    PO _childrenUpdate = instance(clazz);
                    _childrenUpdate.setParentId(e.getParentId());
                    _childrenUpdate.setOrganizationId(dto.getOrganizationId());
                    Flux<PO> childrenUpdate =  super.find(_childrenUpdate).flatMap(x->{
                        x.setParentId(e.getParentId());
                        return Flux.just(x);
                    });
                    return Flux.concat(childrenUpdate,deleteSelf).then();
                }).then();
    }
}
