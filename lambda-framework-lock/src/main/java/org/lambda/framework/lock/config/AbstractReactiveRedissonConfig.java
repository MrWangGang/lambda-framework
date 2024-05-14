package org.lambda.framework.lock.config;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.lock.ReactiveRedissonBuilder;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static org.lambda.framework.lock.enums.LockExceptionEnum.*;

public abstract class AbstractReactiveRedissonConfig {
    //##Redis服务器地址
    protected abstract String host();
    //## Redis服务器连接端口
    protected abstract String port();
    //连接池密码
    protected abstract String password();
    //##数据库序号
    protected abstract Integer database();

    protected ReactiveRedissonBuilder buildSingleClient(){
        //密码不需要校验
        Assert.verify(this.host(),ES_LOCK_REDISSON_000);
        Assert.verify(this.port(),ES_LOCK_REDISSON_001);
        Assert.verify(this.database(),ES_LOCK_REDISSON_006);

        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+host() + ":" + port())
                .setDatabase(this.database())
                .setPassword(password());
        RedissonClient redisson = Redisson.create(config);
        return new ReactiveRedissonBuilder(redisson.reactive());
    }
}
