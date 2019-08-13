package com.sanri.app.serializer;

import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HexSerializer implements ZkSerializer {
    private Logger logger = LoggerFactory.getLogger(HexSerializer.class);
    @Override
    public byte[] serialize(Object o) throws ZkMarshallingError {
        if (o == null)
            return new byte[0];
        //只能转换字符串
        if(o instanceof String){
            String source = (String) o;
            char[] chars = source.toCharArray();
            try {
                return Hex.decodeHex(chars);
            } catch (DecoderException e) {
                return new byte[0];
            }
        }
        logger.error("hex 只支持字符串序列化 ");
        return new byte[0];
    }

    @Override
    public Object deserialize(byte[] bytes) throws ZkMarshallingError {
        return new String(Hex.encodeHex(bytes));
    }
}
