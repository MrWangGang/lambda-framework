package org.lambda.framework.web.handler;

import org.lambda.framework.common.templete.ResponseTemplete;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class WebResponseHandler {
/*    @Deprecated
    public static Mono<ServerResponse> ok(Mono data){
        return ServerResponse.ok().contentType(MEDIATYPE).body(handlerMono(data), ResponseTemplete.class);
    }
    @Deprecated
    public static Mono<ServerResponse> ok(Flux data){
        return ServerResponse.ok().contentType(MEDIATYPE).body(handlerFlux(data), ResponseTemplete.class);
    }

    @Deprecated
    public static Mono<ServerResponse> ok(){
        ResponseTemplete responseTemplete = new ResponseTemplete();
        return ServerResponse.ok().contentType(MEDIATYPE).body(Mono.just(responseTemplete), ResponseTemplete.class);
    }*/

    protected  Mono<ResponseTemplete> ok(Mono data){
        return handlerMono(data);
    }
    protected  Mono<ResponseTemplete> ok(Flux data){
        return handlerFlux(data);
    }

    protected   Mono<ResponseTemplete> ok(){
        return Mono.just(new ResponseTemplete());
    }

    private  Mono<ResponseTemplete> handlerMono(Mono data){
        return data.flatMap(e->{
           return  Mono.just(new ResponseTemplete(e));
        });
    }

    private  Mono<ResponseTemplete> handlerFlux(Flux data){
        return data.collectList().flatMap(e->{
            return  Mono.just(new ResponseTemplete(e));
        });
    }
}
