package org.lambda.framework.rsocket.adapter;

import java.util.List;

/** Contract types that an application allows on its Fory RPC connections. */
public record ForyRpcTypes(List<Class<?>> types) {
    public ForyRpcTypes {
        types = List.copyOf(types);
    }
}
