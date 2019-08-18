package com.sanri.app.serializer;

import com.alibaba.fastjson.JSON;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.apache.commons.lang3.ObjectUtils;

public class FastJsonSerializer extends StringSerializer {
    @Override
    public byte[] serialize(Object data) throws ZkMarshallingError {
        String jsonString = JSON.toJSONString(data);
        return super.serialize(jsonString);
    }

    @Override
    public Object deserialize(byte[] bytes) throws ZkMarshallingError {
        String deserialize = ObjectUtils.toString(super.deserialize(bytes));
        return JSON.parseObject(deserialize);
    }
}
