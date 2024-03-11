package org.lambda.framework.gateway.filter.support;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.templete.ResponseTemplete;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.lambda.framework.gateway.enums.GatewayContract.*;
import static org.lambda.framework.gateway.enums.GatewayExceptionEnum.*;

@Component
public class RsocketRequestFactory {

    @Resource
    private RSocketLoadbalance rSocketLoadbalance;
    public Mono<Void> execute(ServerWebExchange exchange, GatewayFilterChain chain,String match,RSocketRequesterBuild rSocketRequesterBuild){
        Assert.verify(match,ES_GATEWAY_005);
        // 获取请求的 URL
        URI targetUri = (URI)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        Assert.verify(targetUri.getHost(),ES_GATEWAY_008);
        Assert.verify(targetUri,ES_GATEWAY_001);
        Assert.verify(targetUri.getScheme(),ES_GATEWAY_001);
        URI currentUri = exchange.getRequest().getURI();
        String _scheme = currentUri.getScheme();
        Assert.verify(currentUri.getScheme(),ES_GATEWAY_002);
        //如果请求网关的协议是http开头的
        if(_scheme.equals(HTTP_SCHEME) || _scheme.equals(HTTPS_SCHEME)){
            // 如果 目标URL 以 rb:// 开头，则进行 RSocket 负载均衡的 转发
            if (targetUri != null && (match.equals(targetUri.getScheme()))) {
                HttpHeaders reqHeaders = exchange.getRequest().getHeaders();
                Assert.verify(reqHeaders,ES_GATEWAY_005);
                //获取model
                List<String> rsocketModelHeaders = reqHeaders.get(RSOCKET_MODEL);
                Assert.verify(rsocketModelHeaders,ES_GATEWAY_005);
                String rsocketModel = rsocketModelHeaders.getFirst();
                Assert.verify(rsocketModel,ES_GATEWAY_005);
                //获取echo
                List<String> rsocketEchoHeaders = reqHeaders.get(RSOCKET_ECHO);
                Assert.verify(rsocketEchoHeaders,ES_GATEWAY_005);
                String rsocketEcho = rsocketEchoHeaders.getFirst();
                Assert.verify(rsocketModel,ES_GATEWAY_009);
                //获取请求里的body
                Flux<DataBuffer> bodyFlux = exchange.getRequest().getBody();
                ServerHttpResponse rs = exchange.getResponse();
                return extractRequestBody(bodyFlux)
                        .switchIfEmpty(Mono.error(new EventException(ES_GATEWAY_004)))
                        //bodyByte是绝对不会为空的 。extract永远都会返回一个;
                        .flatMap(body->{
                            Mono<RSocketRequester> rSocketRequester = rSocketRequesterBuild.build(rSocketLoadbalance,targetUri.getHost(),targetUri.getPort());
                            return rSocketRequester.flatMap(requester->{
                                // 使用RSocket客户端发送请求
                                RSocketRequester.RetrieveSpec retrieveSpec =  requester
                                        .route(targetUri.getPath())
                                        .data(body);
                                switch (rsocketModel){
                                    case RSOCKET_MODEL_REQUEST_RESPONSE, RSOCKET_MODEL_FIRE_AND_FORGET:
                                        switch (rsocketEcho){
                                            case RSOCKET_ECHO_CHAR, RSOCKET_ECHO_ARRAY:
                                                return handleResponse(retrieveSpec.retrieveMono(String.class), rs);
                                            case RSOCKET_ECHO_OBJECT, RSOCKET_ECHO_MEDIA:
                                                return handleResponse(retrieveSpec.retrieveMono(Object.class), rs);
                                            default:
                                                return Mono.error(new EventException(ES_GATEWAY_010));
                                        }
                                    case RSOCKET_MODEL_REQUEST_STREAM, RSOCKET_MODEL_CHANNEL:
                                        switch (rsocketEcho){
                                            case RSOCKET_ECHO_CHAR, RSOCKET_ECHO_ARRAY:
                                                return handleResponse(retrieveSpec.retrieveFlux(String.class).collectList(), rs);
                                            case RSOCKET_ECHO_OBJECT, RSOCKET_ECHO_MEDIA:
                                                return handleResponse(retrieveSpec.retrieveFlux(Object.class).collectList(), rs);
                                            default:
                                                return Mono.error(new EventException(ES_GATEWAY_010));
                                        }
                                    default: return Mono.error(new EventException(ES_GATEWAY_006));
                                }
                            });
                        });
            }
            return chain.filter(exchange);
        }
        throw new EventException(ES_GATEWAY_002);
    }

    private <T> Mono<Void> handleResponse(Mono<?> fluxMono, ServerHttpResponse response) {
        return fluxMono.flatMap(obj -> {
            ResponseTemplete responseTemplete = new ResponseTemplete(obj);
            // 将字符串转换为字节数组
            byte[] responseBytes = JsonUtil.objToString(responseTemplete).getBytes(StandardCharsets.UTF_8);
            // 使用 DefaultDataBufferFactory 创建 DataBuffer
            DataBuffer joinedDataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(responseBytes);
            return response.writeWith(Mono.just(joinedDataBuffer));
        }).switchIfEmpty(Mono.defer(()->{
            ResponseTemplete responseTemplete = new ResponseTemplete();
            // 将字符串转换为字节数组
            byte[] responseBytes = JsonUtil.objToString(responseTemplete).getBytes(StandardCharsets.UTF_8);
            // 使用 DefaultDataBufferFactory 创建 DataBuffer
            DataBuffer joinedDataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(responseBytes);
            return response.writeWith(Mono.just(joinedDataBuffer));
        }));
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

    public interface RSocketRequesterBuild{
        public Mono<RSocketRequester> build(RSocketLoadbalance rSocketLoadbalance,String host,Integer port);
    }
}
