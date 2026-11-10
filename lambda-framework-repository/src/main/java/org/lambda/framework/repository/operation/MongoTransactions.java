package org.lambda.framework.repository.operation;

import com.mongodb.MongoException;

public final class MongoTransactions {
    private MongoTransactions() {}

    public static boolean isTransientFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof MongoException mongo
                    && mongo.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)) {
                return true;
            }
        }
        return false;
    }
}
