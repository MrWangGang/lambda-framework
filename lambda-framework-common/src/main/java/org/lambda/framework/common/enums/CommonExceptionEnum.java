package org.lambda.framework.common.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public enum CommonExceptionEnum implements ExceptionEnumFunction {
    ES_COMMON_000("ES_COMMON_000","未知错误"),
    ES_COMMON_001("ES_COMMON_001","链接点参数为空"),
    ES_COMMON_002("ES_COMMON_002","类或对象访问权限限制"),
    ES_COMMON_003("ES_COMMON_003","I/O异常"),
    ES_COMMON_004("ES_COMMON_004","不支持字符编码"),
    ES_COMMON_005("ES_COMMON_005","没有此算法"),
    ES_COMMON_006("ES_COMMON_006","缺少算法配置参数"),
    ES_COMMON_007("ES_COMMON_007","读取私钥失败"),
    ES_COMMON_008("ES_COMMON_008","加载私钥失败"),
    ES_COMMON_009("ES_COMMON_009","密文数据已损坏"),
    ES_COMMON_010("ES_COMMON_010","私钥长度非法"),
    ES_COMMON_011("ES_COMMON_011","私钥非法"),
    ES_COMMON_012("ES_COMMON_012","读取公钥失败"),
    ES_COMMON_013("ES_COMMON_013","加载公钥失败"),
    ES_COMMON_014("ES_COMMON_014","明文数据已损坏"),
    ES_COMMON_015("ES_COMMON_015","公钥非法"),
    ES_COMMON_016("ES_COMMON_016","公钥长度非法"),
    ES_COMMON_017("ES_COMMON_017","签名失败"),
    ES_COMMON_018("ES_COMMON_018","JSON转换OBJ失败"),
    ES_COMMON_019("ES_COMMON_019","OBJ转换JSON失败"),
    ES_COMMON_020("ES_COMMON_020","调用失败"),
    ES_COMMON_021("ES_COMMON_021","copy bean异常"),
    ES_COMMON_022("ES_COMMON_022","类型不匹配String"),
    ES_COMMON_023("ES_COMMON_023","要转成date的value不能为空"),
    ES_COMMON_024("ES_COMMON_024","日期转化失败"),
    ES_COMMON_025("ES_COMMON_025","MD5加密失败"),
    ES_COMMON_026("ES_COMMON_026","反射获取属性错误"),
    ES_COMMON_027("ES_COMMON_027","请先配置PrincipalStash"),
    ES_COMMON_028("ES_COMMON_028","beanUtil复制bean失败"),
    ES_COMMON_029("ES_COMMON_029","对象属性必须都被设置"),
    ES_COMMON_030("ES_COMMON_030","深拷贝生成对象cglib对象失败");

    // 成员变量
    private String code;

    private String message;
    // 构造方法
    private CommonExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
