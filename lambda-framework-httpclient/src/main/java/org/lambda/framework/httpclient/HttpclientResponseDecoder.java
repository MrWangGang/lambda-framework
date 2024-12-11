package org.lambda.framework.httpclient;

import com.fasterxml.jackson.databind.JavaType;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.lambda.framework.httpclient.enums.HttpclientExceptionEnum.ES_HTTPCLIENT_000;

public abstract class HttpclientResponseDecoder<T> implements Decoder<T> {

    public abstract Boolean verify(T t);

    @Override
    public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
        return MediaType.APPLICATION_JSON.isCompatibleWith(mimeType);
    }

    @Override
    public Flux<T> decode(Publisher<DataBuffer> inputStream,ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        if (inputStream != null) {
            return Flux.from(inputStream)
                    .map(dataBuffer -> {
                        try {
                            // 将合并后的 DataBuffer 转换为字符串
                            String json = StandardCharsets.UTF_8.decode(dataBuffer.asByteBuffer()).toString();
                            JavaType javaType = JsonUtil.getJsonFactory().getTypeFactory().constructType(elementType.getType());
                            T result =  JsonUtil.getJsonFactory().readValue(json,javaType);
                            if(!this.verify(result)){
                                throw new EventException(ES_HTTPCLIENT_000,"[webclient]响应校验失败");
                            }
                            return result;
                        } catch (Exception e){
                            throw new EventException(ES_HTTPCLIENT_000,"[webclient]响应解析失败");
                        }finally {
                            // 确保释放 DataBuffer
                            DataBufferUtils.release(dataBuffer);
                        }
                    });
        }
        throw new EventException(ES_HTTPCLIENT_000,"[webclient]响应为空,无法解析");
    }

    @Override
    public Mono<T> decodeToMono(Publisher<DataBuffer> inputStream,ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        if (inputStream != null) {
            return Flux.from(inputStream)
                    .reduce(DataBuffer::write)
                    .map(dataBuffer -> {
                        try {
                            // 将合并后的 DataBuffer 转换为字符串
                            String json = StandardCharsets.UTF_8.decode(dataBuffer.asByteBuffer()).toString();
                            JavaType javaType = JsonUtil.getJsonFactory().getTypeFactory().constructType(elementType.getType());
                            T result =  JsonUtil.getJsonFactory().readValue(json,javaType);
                            if(!this.verify(result)){
                                throw new EventException(ES_HTTPCLIENT_000,"[webclient]响应校验失败");
                            }
                            return result;
                        } catch (Exception e){
                            throw new EventException(ES_HTTPCLIENT_000,"[webclient]响应解析失败");
                        }finally {
                            // 确保释放合并后的 DataBuffer
                            DataBufferUtils.release(dataBuffer);
                        }
                    });
        }
        throw new EventException(ES_HTTPCLIENT_000,"[webclient]响应为空,无法解析");
    }

    @Override
    public List<MimeType> getDecodableMimeTypes() {
        return List.of(MimeTypeUtils.APPLICATION_JSON);
    }
}
