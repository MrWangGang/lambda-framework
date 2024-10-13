package org.lambda.framework.common.util.sample;

import org.lambda.framework.common.exception.EventException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_035;

public class ObjectUtil {
    public static List<byte[]> serializeObject(Object object) {
        List<byte[]> byteList = new ArrayList<>();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            byteList.add(bos.toByteArray());
        } catch (IOException e) {
            throw new EventException(ES_COMMON_035);
        }
        return byteList;
    }
}
