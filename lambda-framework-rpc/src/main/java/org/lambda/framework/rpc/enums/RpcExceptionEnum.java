package org.lambda.framework.rpc.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum RpcExceptionEnum implements ExceptionEnumFunction {

    //系统异常-普通异常 0-99
    ES_RPC_000("ES_RPC_000","@RsocketRpc注解只能标记在类型为接口[interface]的属性上"),
    ES_RPC_001("ES_RPC_001","@RsocketRpc注解标记在接口不能继承其他接口"),
    ES_RPC_002("ES_RPC_002","@RsocketRpc注解标记在接口必须声明@RsocketRpcDiscorvery来标注需要调用的远程服务信息"),
    ES_RPC_003("ES_RPC_003","@RsocketRpcDiscorvery注解声明的接口里的每个方法都必须由@RSocketRpcMapping来声明"),
    ES_RPC_004("ES_RPC_004","@RsocketRpcDiscorvery注解声明中的链路地址或服务名不能为空"),
    ES_RPC_005("ES_RPC_005","@RsocketRpc中的value必须是ip:port形式或者纯字母组成的服务名称"),
    ES_RPC_006("ES_RPC_006","@RSocketRpcMapping中的理由地址不能为空"),
    ES_RPC_007("ES_RPC_007","Rsocket连接类型不能为空"),
    ES_RPC_008("ES_RPC_008","Rsocket路由地址不能为空"),
    ES_RPC_009("ES_RPC_009","Rsocket元数据接口类型不存在"),
    ES_RPC_010("ES_RPC_010","Rsocket rpc调用MimeType只支持application/json或application/octet-stream"),
    ES_RPC_011("ES_RPC_011","Rsocket rpc调用MimeType 不能为空"),
    ES_RPC_012("ES_RPC_012","Rsocket rpc直连模式的ip不合规"),
    ES_RPC_013("ES_RPC_013","无效的Rsocket rpc客户端"),
    ES_RPC_014("ES_RPC_014","远程调用的服务所提供的入参只能有1个"),
    ES_RPC_015("ES_RPC_015","返回类型必须是响应式的Mono<?>或Flux<?>,? 必须要指定类型"),
    ES_RPC_016("ES_RPC_016","将@RsocketRpc标注的接口注入到属性失败"),
    ES_RPC_017("ES_RPC_017","获取@RsocketRpc标注的属性，递归获取失败"),
    ES_RPC_018("ES_RPC_018","ASM没有找到class,无法操作字节码"),
    ES_RPC_019("ES_RPC_019","ASM读取字节码失败"),
    ES_RPC_020("ES_RPC_020","ASM获取类路径失败"),
    ES_RPC_021("ES_RPC_020","ASM获取类路径包失败"),
    ES_RPC_022("ES_RPC_022","@SpringBootApplication 没有表记载启动类上，无法获取扫描路径"),
    ES_RPC_023("ES_RPC_023","只能继承一个@RsocketRpcDiscorvery标注的接口"),
    ES_RPC_024("ES_RPC_024","@RsocketRpcDiscorvery的实现类同名的方法不能添加@MessageMaping"),
    ES_RPC_025("ES_RPC_025","@RsocketRpcDiscorvery的实现类只能由@RSocketApi来进行标注"),
    ES_RPC_026("ES_RPC_026","@EnableRSocketRpcDiscorvery注解在启动类上没有找到"),
    ES_RPC_027("ES_RPC_027","rpc无法确定返回类型");







// 成员变量

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String message;
    // 构造方法
    private RpcExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
}
