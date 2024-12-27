package org.lambda.framework.repository.config.elasticsearch;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.springframework.beans.factory.annotation.Value;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public class DefaultReactiveElasticsearchRepositoryConfig extends AbstractReactiveElasticsearchRepositoryConfig{
    private String host; // 主机地址（支持多个）
    private String user; // 用户名
    private String password; // 密码
    @Value("${lambda.repository.elasticsearch.connect-timeout-seconds:60}")
    private Integer connectTimeoutSeconds; // 连接超时时间
    @Value("${lambda.repository.elasticsearch.socket-timeout-seconds:60}")
    private Integer socketTimeoutSeconds; // 读写超时时间

    @Value("${lambda.repository.elasticsearch.host:-1}")
    private void setHost(String host) {
        if(host == null || StringUtils.isBlank(host) || "-1".equals(host)){
            throw new EventException(ES_REPOSITORY_ELASTICSEARCH_020);
        }
        this.host = host;
    }

    @Value("${lambda.repository.elasticsearch.user:-1}")
    private void setUser(String user) {
        if(user == null || StringUtils.isBlank(user) || "-1".equals(user)){
            throw new EventException(ES_REPOSITORY_ELASTICSEARCH_021);
        }
        this.user = user;
    }

    @Value("${lambda.repository.elasticsearch.password:-1}")
    private void setPassword(String password) {
        if(password == null || StringUtils.isBlank(password) ||"-1".equals(password)){
            throw new EventException(ES_REPOSITORY_ELASTICSEARCH_022);
        }
        this.password = password;
    }



    @Override
    protected String host() {
        return this.host;
    }

    @Override
    protected String user() {
        return this.user;
    }

    @Override
    protected String password() {
        return this.password;
    }

    @Override
    protected Integer connectTimeoutSeconds() {
        return this.connectTimeoutSeconds;
    }

    @Override
    protected Integer socketTimeoutSeconds() {
        return this.socketTimeoutSeconds;
    }
}
