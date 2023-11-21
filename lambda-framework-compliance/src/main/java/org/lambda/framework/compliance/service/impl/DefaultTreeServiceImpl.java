package org.lambda.framework.compliance.service.impl;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.repository.po.IFlattenTreePO;
import org.lambda.framework.compliance.repository.po.UnifyPO;
import org.lambda.framework.compliance.service.IDefaultTreeService;
import org.lambda.framework.compliance.service.dto.MoveNodeDTO;
import org.lambda.framework.repository.operation.mysql.ReactiveMySqlCrudRepositoryOperation;
import org.lambda.framework.security.SecurityPrincipalUtil;
import org.lambda.framework.security.container.SecurityLoginUser;
import org.springframework.data.domain.Example;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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
    public Mono<List<PO>> buildTree(Class<PO> clazz) {
        return getLoginUser(SecurityLoginUser.class).flatMapMany(e->{
            PO po = this.instance(clazz);
            po.setOrganizationId(e.getOrganizationId());
            return Mono.just(po);
        }).flatMap(e->{
            Example<PO> example = Example.of(e);
            return repository.findAll(example);
        }).collectList().flatMap(e->{
            return Mono.just(process(e, ROOT_NODE_DEFAULT));
        });
    }

    private <PO extends IFlattenTreePO>List<PO> process(List<PO> flattenStream, Long parentId) {
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
    public Mono<Void> moveNode(Class<PO> clazz, MoveNodeDTO moveNodeDTO) {
        if(moveNodeDTO.getTargetNodeId() == null || moveNodeDTO.getTargetNodeId().longValue() < ROOT_NODE_DEFAULT)throw new EventException(ES_COMPLIANCE_010);
        if(moveNodeDTO.getCurrentNodeId() == null)throw new EventException(ES_COMPLIANCE_011);
        if(moveNodeDTO.getOrganizationId() == null)throw new EventException(ES_COMPLIANCE_013);
        PO _currentNode =  instance(clazz);
        _currentNode.setId(moveNodeDTO.getCurrentNodeId());
        _currentNode.setOrganizationId(moveNodeDTO.getOrganizationId());
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
                    if (moveNodeDTO.getTargetNodeId().equals(ROOT_NODE_DEFAULT)) {
                        PO _current =  instance(clazz);
                        _current.setId(e.getParentId());
                        _current.setOrganizationId(moveNodeDTO.getOrganizationId());
                        return super.find(_current).flatMap(root -> {
                            //先记录之前的parentId,用来改变原先的子节点的挂靠
                            Long oldParentId = e.getParentId();
                            //将当前节点变成根节点
                            e.setParentId(ROOT_NODE_DEFAULT);
                            //查出当前变动节点的子孙节点
                            PO _children = instance(clazz);
                            _children.setParentId(e.getId());
                            _children.setOrganizationId(moveNodeDTO.getOrganizationId());
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
                    _target.setId(moveNodeDTO.getTargetNodeId());
                    _target.setOrganizationId(moveNodeDTO.getOrganizationId());
                    return super.get(_target)
                            .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_007)))
                            .then(Mono.just(e)).flatMap(x->{
                                Long oldParentId = x.getParentId();
                                x.setParentId(moveNodeDTO.getTargetNodeId());
                                Mono<Void> currentSelfUpdate = super.update(x).then();
                                PO  _children = instance(clazz);
                                _children.setParentId(x.getId());
                                _children.setOrganizationId(moveNodeDTO.getOrganizationId());
                                Flux<PO> currentChildren = super.find(_children)
                                        .flatMap(y->{
                                            //排除自己
                                            y.setParentId(oldParentId);
                                            return Flux.just(y);
                                        });
                                Mono<Void> currentChildrenUpdate = super.update(currentChildren).then();
                                return Flux.concat(currentChildrenUpdate,currentSelfUpdate).then();
                            });
                });
    }
}
