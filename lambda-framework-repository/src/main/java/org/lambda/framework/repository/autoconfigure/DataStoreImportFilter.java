package org.lambda.framework.repository.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

final class DataStoreImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean[] match(String[] configurations, AutoConfigurationMetadata metadata) {
        boolean mongo = StringUtils.hasText(environment.getProperty("lambda.repository.mongo.uri"));
        boolean redis = environment.containsProperty("lambda.repository.redis.nodes")
                || environment.containsProperty("lambda.repository.redis.nodes[0]");
        boolean[] matches = new boolean[configurations.length];
        for (int i = 0; i < configurations.length; i++) {
            String name = configurations[i];
            matches[i] = name != null && enabled(name, mongo, redis);
        }
        return matches;
    }

    private boolean enabled(String name, boolean mongo, boolean redis) {
        if (name.startsWith("org.springframework.boot.mongodb.autoconfigure.")
                || name.startsWith("org.springframework.boot.data.mongodb.autoconfigure.")) {
            return mongo && !name.endsWith(".MongoAutoConfiguration")
                    && !name.endsWith(".DataMongoAutoConfiguration")
                    && !name.endsWith(".DataMongoRepositoriesAutoConfiguration");
        }
        if (name.startsWith("org.springframework.boot.data.redis.autoconfigure.")) {
            return redis && !name.endsWith(".DataRedisRepositoriesAutoConfiguration");
        }
        return true;
    }
}
