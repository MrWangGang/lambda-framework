package org.lamb.framework.web.core.handler;

import org.lamb.framework.common.templete.LambResponseTemplete;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public abstract class LambResponseHandler {

    protected Mono<ServerResponse> routing(Collection data){
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(handlerFlux(Flux.just(data)),LambResponseTemplete.class);
    }
    protected Mono<ServerResponse> routing(Object data){
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(handlerMono(Mono.just(data)),LambResponseTemplete.class);
    }
    protected Mono<ServerResponse> routing(Mono data){
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(handlerMono(data),LambResponseTemplete.class);
    }

    protected Mono<ServerResponse> routing(Flux data){
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(handlerFlux(data),LambResponseTemplete.class);
    }

    protected Mono<ServerResponse> routing(){
        LambResponseTemplete lambResponseTemplete = new LambResponseTemplete();
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(lambResponseTemplete,LambResponseTemplete.class);
    }
    protected Mono<LambResponseTemplete> returning(Collection data){
        return handlerFlux(Flux.just(data));
    }
    protected Mono<LambResponseTemplete> returning(Object data){
        return handlerMono(Mono.just(data));
    }

    protected Mono<LambResponseTemplete> returning(Mono data){
        return handlerMono(data);
    }
    protected Mono<LambResponseTemplete> returning(Flux data){
        return handlerFlux(data);
    }

    protected Mono<LambResponseTemplete> returning(){
        return Mono.just(new LambResponseTemplete());
    }

    private Mono<LambResponseTemplete> handlerMono(Mono data){
        return data.flatMap(e->{
           return  Mono.just(new LambResponseTemplete(e));
        });
    }

    private Mono<LambResponseTemplete> handlerFlux(Flux data){
        return data.collectList().flatMap(e->{
            return  Mono.just(new LambResponseTemplete(e));
        });
    }
}
