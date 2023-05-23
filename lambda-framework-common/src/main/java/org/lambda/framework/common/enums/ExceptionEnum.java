package org.lambda.framework.common.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public enum ExceptionEnum implements ExceptionEnumFunction {
    ESYS000001("ESYS000001","链接点参数为空"),
    ESYS000002("ESYS000002","类或对象访问权限限制"),
    ESYS000003("ESYS000003","I/O异常"),
    ESYS000005("ESYS000005","不支持字符编码"),
    ESYS000006("ESYS000006","没有此算法"),
    ESYS000007("ESYS000007","缺少算法配置参数"),
    ESYS000008("ESYS000008","读取私钥失败"),
    ESYS000009("ESYS000009","加载私钥失败"),
    ESYS000010("ESYS000010","密文数据已损坏"),
    ESYS000011("ESYS000011","私钥长度非法"),
    ESYS000012("ESYS000012","私钥非法"),
    ESYS000013("ESYS000013","读取公钥失败"),
    ESYS000014("ESYS000014","加载公钥失败"),
    ESYS000015("ESYS000015","明文数据已损坏"),
    ESYS000016("ESYS000016","公钥非法"),
    ESYS000017("ESYS000017","公钥长度非法"),
    ESYS000018("ESYS000018","签名失败"),
    ESYS000019("ESYS000019","JSON转换OBJ失败"),
    ESYS000020("ESYS000020","OBJ转换JSON失败"),
    ESYS000021("ESYS000021","调用失败"),
    ESYS000022("ESYS000022","BeanPlasticityUtill异常"),
    ESYS000023("ESYS000023","类型不匹配String"),
    ESYS000024("ESYS000024","要转成date的value不能为空"),
    ESYS000025("ESYS000025","日期转化失败"),
    ESYS000026("ESYS000026","MD5加密失败");
    // 成员变量
    private String code;

    private String message;
    // 构造方法
    private ExceptionEnum(String code, String message) {
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
