package org.lambda.framework.repository.config.mongodb;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.springframework.beans.factory.annotation.Value;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public class DefaultReactiveMongoRepositoryConfig extends AbstractReactiveMongoRepositoryConfig {
    private String host;

    private String user;

    private String password;

    private String database;
    @Value("${lambda.repository.mongo.host:-1}")
    private void setHost(String host) {
        if(host == null || StringUtils.isBlank(host) || "-1".equals(host)){
            throw new EventException(ES_REPOSITORY_MONGO_004);
        }
        this.host = host;
    }

    @Value("${lambda.repository.mongo.user:-1}")
    private void setUser(String user) {
        if(user == null || StringUtils.isBlank(user) || "-1".equals(user)){
            throw new EventException(ES_REPOSITORY_MONGO_005);
        }
        this.user = user;
    }

    @Value("${lambda.repository.mongo.password:-1}")
    private void setPassword(String password) {
        if(password == null || StringUtils.isBlank(password) ||"-1".equals(password)){
            throw new EventException(ES_REPOSITORY_MONGO_006);
        }
        this.password = password;
    }

    @Value("${lambda.repository.mongo.database:-1}")
    private void setDatabase(String database) {
        if(database == null || StringUtils.isBlank(database) ||"-1".equals(database)){
            throw new EventException(ES_REPOSITORY_MONGO_007);
        }
        this.database = database;
    }

    @Value("${lambda.repository.mongo.port:27017}")
    private Integer port;
    @Value("${lambda.repository.mongo.connect-timeout-seconds:60}")
    private Integer connectTimeoutSeconds;
    @Value("${lambda.repository.mongo.max-idle-time-seconds:30}")
    private Integer maxIdleTimeSeconds;
    @Value("${lambda.repository.mongo.max-size:50}")
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
