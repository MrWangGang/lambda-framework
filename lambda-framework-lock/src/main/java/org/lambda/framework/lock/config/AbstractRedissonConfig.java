package org.lambda.framework.lock.config;

import org.lambda.framework.common.enums.RedisDeployModelEnum;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.lock.RedissonBuilder;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lambda.framework.lock.enums.LockExceptionEnum.*;

public abstract class AbstractRedissonConfig {
    //##Redis服务器地址
    protected abstract String host();
    //连接池密码
    protected abstract String password();
    //##数据库序号
    protected abstract Integer database();

    protected abstract String deployModel();

    protected abstract String masterName();

    protected RedissonBuilder redissonConnectionFactory(){
        Assert.verify(this.deployModel(),ES_LOCK_REDISSON_008);
        RedisDeployModelEnum.isValid(this.deployModel());
        switch (RedisDeployModelEnum.valueOf(this.deployModel())) {
            case single:
                return single();
            case master_slave:
                return masterSlave();
            case cluster:
                return cluster();
            case sentinel:
                return sentinel();
            default:
                throw new EventException(ES_LOCK_REDISSON_009);
        }
    }

    private RedissonBuilder single(){
        //密码不需要校验
        Assert.verify(this.host(),ES_LOCK_REDISSON_000);
        Assert.verify(this.database(),ES_LOCK_REDISSON_006);
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+host())
                .setDatabase(this.database())
                .setPassword(password());
        RedissonClient redisson = Redisson.create(config);
        return new RedissonBuilder(redisson);
    }

    private RedissonBuilder masterSlave(){
        //密码不需要校验
        Assert.verify(this.host(),ES_LOCK_REDISSON_000);
        Assert.verify(this.database(),ES_LOCK_REDISSON_006);
        Config config = new Config();
        config.useMasterSlaveServers()
                .setDatabase(this.database())
                .setPassword(password());
        String[] nodes = this.host().split(",");
        boolean firstNode = true;
        Set<String> sets = new HashSet<>();
        for (String node : nodes) {
            if (firstNode) {
                config.useMasterSlaveServers().setMasterAddress("redis://"+node);
                firstNode = false;
            } else {
                sets.add("redis://"+node);
            }
        }
        config.useMasterSlaveServers().setSlaveAddresses(sets);
        RedissonClient redisson = Redisson.create(config);
        return new RedissonBuilder(redisson);
    }

    private RedissonBuilder sentinel(){
        //密码不需要校验
        Assert.verify(this.host(),ES_LOCK_REDISSON_000);
        Assert.verify(this.database(),ES_LOCK_REDISSON_006);
        Assert.verify(this.masterName(),ES_LOCK_REDISSON_006);
        Config config = new Config();
        config.useSentinelServers()
                .setMasterName(this.masterName())
                .setDatabase(this.database())
                .setPassword(password());
        String[] nodes = this.host().split(",");
        List<String> list = new ArrayList<>();
        for (String node : nodes) {
            list.add("redis://"+node);
        }
        config.useSentinelServers().setSentinelAddresses(list);
        RedissonClient redisson = Redisson.create(config);
        return new RedissonBuilder(redisson);
    }

    private RedissonBuilder cluster(){
        //密码不需要校验
        Assert.verify(this.host(),ES_LOCK_REDISSON_000);
        Config config = new Config();
        config.useClusterServers()
                .setPassword(password());
        String[] nodes = this.host().split(",");
        List<String> list = new ArrayList<>();
        for (String node : nodes) {
            list.add("redis://" + node);
        }
        config.useClusterServers().setNodeAddresses(list);
        RedissonClient redisson = Redisson.create(config);
        return new RedissonBuilder(redisson);
    }
}
