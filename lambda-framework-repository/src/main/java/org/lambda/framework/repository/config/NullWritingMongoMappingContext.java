package org.lambda.framework.repository.config;

import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.mongodb.core.mapping.CachingMongoPersistentProperty;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;

/** MongoDB mapping context that writes null-valued mapped properties explicitly. */
public final class NullWritingMongoMappingContext extends MongoMappingContext {
    private FieldNamingStrategy namingStrategy = PropertyNameFieldNamingStrategy.INSTANCE;

    @Override
    public void setFieldNamingStrategy(FieldNamingStrategy strategy) {
        super.setFieldNamingStrategy(strategy);
        namingStrategy = strategy == null ? PropertyNameFieldNamingStrategy.INSTANCE : strategy;
    }

    @Override
    public MongoPersistentProperty createPersistentProperty(Property property, MongoPersistentEntity<?> owner,
                                                             SimpleTypeHolder simpleTypes) {
        return new NullWritingProperty(property, owner, simpleTypes, namingStrategy);
    }

    private static final class NullWritingProperty extends CachingMongoPersistentProperty {
        private NullWritingProperty(Property property, MongoPersistentEntity<?> owner, SimpleTypeHolder simpleTypes,
                                    FieldNamingStrategy namingStrategy) {
            super(property, owner, simpleTypes, namingStrategy);
            validate();
        }

        @Override
        public boolean writeNullValues() {
            // MongoDB must continue to generate IDs when an entity has not been assigned one.
            return !isIdProperty() || super.writeNullValues();
        }
    }
}
