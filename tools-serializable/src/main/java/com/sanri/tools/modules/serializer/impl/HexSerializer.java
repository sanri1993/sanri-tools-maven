package com.sanri.tools.modules.serializer.impl;

import com.sanri.tools.modules.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

@Slf4j
public class HexSerializer implements Serializer {
    @Override
    public String name() {
        return "hex";
    }

    @Override
    public byte[] serialize(Object o){
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
        log.error("hex 只支持字符串序列化 ");
        return new byte[0];
    }

    @Override
    public Object deserialize(byte[] bytes,ClassLoader classLoader) {
        return new String(Hex.encodeHex(bytes));
    }
}
