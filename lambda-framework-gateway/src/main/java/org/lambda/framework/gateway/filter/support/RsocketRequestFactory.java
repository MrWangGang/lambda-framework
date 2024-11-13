package org.lambda.framework.gateway.filter.support;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.templete.ResponseTemplete;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.lambda.framework.gateway.enums.GatewayContract.*;
import static org.lambda.framework.gateway.enums.GatewayExceptionEnum.ES_GATEWAY_000;

@Component
public class RsocketRequestFactory {

    @Resource
    private RSocketLoadbalance rSocketLoadbalance;

    public Mono<Void> execute(ServerWebExchange exchange, GatewayFilterChain chain,URI targetUri){
        Assert.verify(targetUri,ES_GATEWAY_000,"未知的请求协议");
        Assert.verify(targetUri.getHost(),ES_GATEWAY_000,"未知的服务名");
        Assert.verify(targetUri.getScheme(),ES_GATEWAY_000,"未知的scheme");
        Assert.verify(targetUri.getPath(),ES_GATEWAY_000,"未知的路由");

        HttpHeaders reqHeaders = exchange.getRequest().getHeaders();
        Assert.verify(reqHeaders,ES_GATEWAY_000,"请求头缺失");
        //获取model
        List<String> rsocketModelHeaders = reqHeaders.get(RSOCKET_MODEL);
        Assert.verify(rsocketModelHeaders,ES_GATEWAY_000,"请求头:RSocket-Model缺失");
        String rsocketModel = rsocketModelHeaders.getFirst();
        Assert.verify(rsocketModel,ES_GATEWAY_000,"请求头:RSocket-Model缺失");
        //获取echo
        List<String> rsocketEchoHeaders = reqHeaders.get(RSOCKET_ECHO);
        Assert.verify(rsocketEchoHeaders,ES_GATEWAY_000,"请求头:RSocket-Echo缺失");
        String rsocketEcho = rsocketEchoHeaders.getFirst();
        Assert.verify(rsocketModel,ES_GATEWAY_000,"请求头:RSocket-Echo缺失");
        //校验headers
        MimeType contentType = this.verifyHttpHeaders(exchange);
        MultiValueMap<String,String> queryParams = exchange.getRequest().getQueryParams();
        if(verifyQueryParamsIsNotNull(queryParams)){
            throw new EventException(ES_GATEWAY_000,"rsocket协议不支持query params");
        }
        Mono<RSocketRequester> rSocketRequester =rSocketLoadbalance.build(targetUri.getHost(),contentType);
        return rSocketRequester.flatMap(requester->{
            //获取请求里的body
            Flux<DataBuffer> bodyFlux = exchange.getRequest().getBody();
            return extractRequestBody(bodyFlux)
                    .switchIfEmpty(Mono.error(new EventException(ES_GATEWAY_000,"request body不合规")))
                    //bodyByte是绝对不会为空的 。extract永远都会返回一个;
                    .flatMap(body-> {
                        // 使用RSocket客户端发送请求
                        RSocketRequester.RetrieveSpec retrieveSpec = null;
                        if (MimeTypeUtils.APPLICATION_JSON.isCompatibleWith(contentType)) {
                            retrieveSpec = requester
                                    .route(targetUri.getPath()).data(body);
                        }else {
                            retrieveSpec = requester
                                    .route(targetUri.getPath()).data(bodyFlux);
                        }
                        ServerHttpResponse rs = exchange.getResponse();
                        switch (rsocketModel){
                            case RSOCKET_MODEL_REQUEST_RESPONSE:
                                switch (rsocketEcho){
                                    case RSOCKET_ECHO_CHAR, RSOCKET_ECHO_VOID :
                                        return handleResponse(retrieveSpec.retrieveMono(String.class), rs);
                                    case RSOCKET_ECHO_OBJECT:
                                        return handleResponse(retrieveSpec.retrieveMono(Object.class), rs);
                                    default:
                                        return Mono.error(new EventException(ES_GATEWAY_000,"请求头:RSocket-Echo无效,仅支持(response/char,response/object,response/void)"));
                                }
                            case RSOCKET_MODEL_REQUEST_STREAM:
                                switch (rsocketEcho){
                                    case RSOCKET_ECHO_CHAR, RSOCKET_ECHO_VOID :
                                        return handleResponse(retrieveSpec.retrieveFlux(String.class).collectList(), rs);
                                    case RSOCKET_ECHO_OBJECT:
                                        return handleResponse(retrieveSpec.retrieveFlux(Object.class).collectList(), rs);
                                    default:
                                        return Mono.error(new EventException(ES_GATEWAY_000,"请求头:RSocket-Echo无效,仅支持(response/char,response/object,response/void)"));
                                }
                            default:
                                return Mono.error(new EventException(ES_GATEWAY_000,"请求头:RSocket-Model无效,仅支持(request/response,request/stream)"));
                        }
                    });
               }).then(chain.filter(exchange));
    }

    private boolean verifyQueryParamsIsNotNull(Map queryParams){
        if(queryParams!=null){
            if(!queryParams.isEmpty()){
                return true;
            }
            return false;
        }
        return false;
    }

    private <T> Mono<Void> handleResponse(Mono<?> fluxMono, ServerHttpResponse response) {
        return fluxMono.flatMap(obj -> {
            ResponseTemplete responseTemplete = new ResponseTemplete(obj);
            // 将字符串转换为字节数组
            byte[] responseBytes = JsonUtil.objToString(responseTemplete).getBytes(StandardCharsets.UTF_8);
            // 使用 DefaultDataBufferFactory 创建 DataBuffer
            DataBuffer joinedDataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(responseBytes);
            return response.writeWith(Mono.just(joinedDataBuffer));
        });
    }

    private Mono<byte[]> extractRequestBody(Flux<DataBuffer> body) {
        if(body == null)return Mono.just(new byte[0]);
        return DataBufferUtils.join(body)
                .flatMap(dataBuffer -> {
                    byte[] contentBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(contentBytes);
                    DataBufferUtils.release(dataBuffer);
                    return Mono.just(contentBytes);
                }).defaultIfEmpty(new byte[0]);
    }


    private MimeType verifyHttpHeaders(ServerWebExchange exchange){
        HttpHeaders reqHeaders = exchange.getRequest().getHeaders();
        Assert.verify(reqHeaders,ES_GATEWAY_000,"请求头缺失");
        //获取content type
        List<String> contentHeaders = reqHeaders.get(HttpHeaders.CONTENT_TYPE);
        Assert.verify(contentHeaders,ES_GATEWAY_000,"请求头:Content-Type缺失");
        String contentType = contentHeaders.getFirst();
        Assert.verify(contentType,ES_GATEWAY_000,"请求头:Content-Type缺失");
        boolean flag = false;
        if(MimeTypeUtils.APPLICATION_JSON.isCompatibleWith(MediaType.valueOf(contentType)) || MimeTypeUtils.APPLICATION_OCTET_STREAM.isCompatibleWith(MediaType.valueOf(contentType)) ){
            flag = true;
        }
        if(!flag){
            throw new EventException(ES_GATEWAY_000,"请求头Content-Type仅支持application/json与application/octet-stream");
        }

        return MimeType.valueOf(contentType);
    }
}
