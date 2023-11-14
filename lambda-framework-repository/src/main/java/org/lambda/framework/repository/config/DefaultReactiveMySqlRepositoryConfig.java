package org.lambda.framework.repository.config;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.springframework.beans.factory.annotation.Value;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public class DefaultReactiveMySqlRepositoryConfig  extends AbstractReactiveMySqlRepositoryConfig  {
    private String host;

    private String user;

    private String password;

    private String database;
    @Value("${lambda.repository.mysql.host:-1}")
    public void setHost(String host) {
        if(host == null || StringUtils.isBlank(host) || "-1".equals(host)){
            throw new EventException(ES_REPOSITORY_MYSQL_000);
        }
        this.host = host;
    }

    @Value("${lambda.repository.mysql.user:-1}")
    public void setUser(String user) {
        if(user == null || StringUtils.isBlank(user) || "-1".equals(user)){
            throw new EventException(ES_REPOSITORY_MYSQL_001);
        }
        this.user = user;
    }

    @Value("${lambda.repository.mysql.password:-1}")
    public void setPassword(String password) {
        if(password == null || StringUtils.isBlank(password) ||"-1".equals(password)){
            throw new EventException(ES_REPOSITORY_MYSQL_002);
        }
        this.password = password;
    }

    @Value("${lambda.repository.mysql.database:-1}")
    public void setDatabase(String database) {
        if(database == null || StringUtils.isBlank(database) ||"-1".equals(database)){
            throw new EventException(ES_REPOSITORY_MYSQL_003);
        }
        this.database = database;
    }

    @Value("${lambda.repository.mysql.port:3306}")
    private Integer port;
    @Value("${lambda.repository.mysql.connect-timeout-seconds:60}")
    private Integer connectTimeoutSeconds;
    @Value("${lambda.repository.mysql.max-idle-time-seconds:30}")
    private Integer maxIdleTimeSeconds;
    @Value("${lambda.repository.mysql.max-size:50}")
    private Integer maxSize;

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
    protected String database() {
        return this.database;
    }

    @Override
    protected Integer port() {
        return this.port;
    }

    @Override
    protected Integer connectTimeoutSeconds() {
        return this.connectTimeoutSeconds;
    }

    @Override
    protected Integer maxIdleTimeSeconds() {
        return this.maxIdleTimeSeconds;
    }

    @Override
    protected Integer maxSize() {
        return this.maxSize;
    }
}
