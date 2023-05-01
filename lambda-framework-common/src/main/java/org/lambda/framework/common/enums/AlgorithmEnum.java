package org.lambda.framework.common.enums;

/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public enum AlgorithmEnum {
    /**RSA1024加密算法*/
    RSA1024("rsa1024", "RSA1024加密算法"),

    /**RSA2048加密算法*/
    RSA2048("rsa2048", "RSA2048加密算法"),

    /**AES对称加密算法*/
    AES("aes", "AES对称加密算法"),

    SHA1WITHRSA("RSA", "SHA1withRSA"),

    SHA256WITHRSA("RSA2", "SHA256withRSA");

    /**code*/
    private String code;

    /**描述*/
    private String desc;

    private AlgorithmEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
