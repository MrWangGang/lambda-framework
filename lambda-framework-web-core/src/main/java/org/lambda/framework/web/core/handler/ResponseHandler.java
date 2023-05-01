package org.lambda.framework.web.core.handler;

import org.lambda.framework.common.templete.ResponseTemplete;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public abstract class ResponseHandler {

    protected Mono<ServerResponse> routing(Collection data){
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(handlerFlux(Flux.just(data)), ResponseTemplete.class);
    }
    protected Mono<ServerResponse> routing(Object data){
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(handlerMono(Mono.just(data)), ResponseTemplete.class);
    }
    protected Mono<ServerResponse> routing(Mono data){
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(handlerMono(data), ResponseTemplete.class);
    }

    protected Mono<ServerResponse> routing(Flux data){
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(handlerFlux(data), ResponseTemplete.class);
    }

    protected Mono<ServerResponse> routing(){
        ResponseTemplete responseTemplete = new ResponseTemplete();
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(responseTemplete, ResponseTemplete.class);
    }
    protected Mono<ResponseTemplete> returning(Collection data){
        return handlerFlux(Flux.just(data));
    }
    protected Mono<ResponseTemplete> returning(Object data){
        return handlerMono(Mono.just(data));
    }

    protected Mono<ResponseTemplete> returning(Mono data){
        return handlerMono(data);
    }
    protected Mono<ResponseTemplete> returning(Flux data){
        return handlerFlux(data);
    }

    protected Mono<ResponseTemplete> returning(){
        return Mono.just(new ResponseTemplete());
    }

    private Mono<ResponseTemplete> handlerMono(Mono data){
        return data.flatMap(e->{
           return  Mono.just(new ResponseTemplete(e));
        });
    }

    private Mono<ResponseTemplete> handlerFlux(Flux data){
        return data.collectList().flatMap(e->{
            return  Mono.just(new ResponseTemplete(e));
        });
    }
}
