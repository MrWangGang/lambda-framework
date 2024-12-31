package org.lambda.framework.repository.config.mongodb;

import com.mongodb.reactivestreams.client.MongoClient;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public class DefaultReactiveMongoRepositoryConfig extends AbstractReactiveMongoRepositoryConfig {

    @Bean
    public MongoClient mongo(){
        return super.buildMongoOperation();
    }


    private String host;

    private String user;

    private String password;

    private String database;

    private String authDatabase;

    private String replicaSetName;

    private String deployModel;

    @Value("${lambda.repository.mongo.deploy:-1}")
    public void setDeployModel(String deployModel) {
        if(deployModel == null || StringUtils.isBlank(deployModel) || "-1".equals(deployModel)){
            throw new EventException(ES_REPOSITORY_MONGO_010);
        }
        this.deployModel = deployModel;
    }

    @Value("${lambda.repository.mongo.replica:-1}")
    public void setReplicaSetName(String replicaSetName) {
        if(replicaSetName == null || StringUtils.isBlank(replicaSetName) || "-1".equals(replicaSetName)){
            throw new EventException(ES_REPOSITORY_MONGO_009);
        }
        this.replicaSetName = replicaSetName;
    }

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

    @Value("${lambda.repository.mongo.auth-database:-1}")
    protected void setAuthDatabase(String authDatabase) {
        if(authDatabase == null || StringUtils.isBlank(authDatabase) ||"-1".equals(authDatabase)){
            throw new EventException(ES_REPOSITORY_MONGO_008);
        }
         this.authDatabase = authDatabase;
    }
    @Override
    protected String authDatabase() {
        return this.authDatabase;
    }

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

    @Override
    protected String replicaSetName() {
        return this.replicaSetName;
    }

    @Override
    protected String deployModel() {
        return this.deployModel;
    }

}
