package com.sanri.app.translate;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kafka.utils.Json;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sanri.utils.HttpUtil;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 有道英语翻译
 */
public class YoudaoEnglishTranslate implements Translate, EnglishTranslate {
    private static final String YOUDAO_URL = "http://openapi.youdao.com/api";

    private static final String APP_KEY = "28a343197650d05a";

    private static final String APP_SECRET = "hioXXfQjbWnRDuLrvbvx3tGpo7hDUpHt";

    private Log logger = LogFactory.getLog(getClass());

    @Override
    public void doTranslate(TranslateCharSequence translateCharSequence, TranslateChain translateChain) {
        //添加直译
        Set<String> translates = directTranslate(translateCharSequence.getOriginSequence().toString());
        if(CollectionUtils.isNotEmpty(translates)){
            for (String translate : translates) {
                translateCharSequence.addDirectTranslate(translate);
            }
        }

        //翻译未翻译过的词
        Set<String> needTranslateWords = translateCharSequence.getNeedTranslateWords();
        if(CollectionUtils.isNotEmpty(needTranslateWords)){
            for (String needTranslateWord : needTranslateWords) {
                Set<String> values = directTranslate(needTranslateWord);
                if(CollectionUtils.isNotEmpty(values)){
                    for (String value : values) {
                        translateCharSequence.addTranslate(needTranslateWord,value);
                    }
                }
            }
        }

        translateChain.doTranslate(translateCharSequence,translateChain);
    }


    @Override
    public Set<String> directTranslate(String source) {
        HashSet<String> values = new LinkedHashSet<String>();

        Map<String,Object> params = new HashMap<String,Object>();
        String q = source;
        String salt = String.valueOf(System.currentTimeMillis());
        params.put("from", "auto");
        params.put("to", "auto");
        params.put("signType", "v3");
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        params.put("curtime", curtime);
        String signStr = APP_KEY + truncate(q) + salt + curtime + APP_SECRET;
        String sign = getDigest(signStr);
        params.put("appKey", APP_KEY);
        params.put("q", q);
        params.put("salt", salt);
        params.put("sign", sign);

        JSONObject jsonObject = null;
        try {
            jsonObject = HttpUtil.postJSON(YOUDAO_URL, new JSONObject(params));
            Integer errorCode = jsonObject.getInteger("errorCode");
            if(errorCode != 0){
                logger.error("调用有道翻译出错:"+errorCode);
                return values;
            }

            //获取翻译结果
            JSONArray translations = jsonObject.getJSONArray("translation");
            for (int i = 0; i < translations.size(); i++) {
                String translation = translations.getString(i);
                String convert2aB = TranslateSupport.convert2aB(translation);
                values.add(convert2aB);
            }

            //获取基础翻译结果
//            JSONObject basic = jsonObject.getJSONObject("basic");
//            if(basic != null){
//                JSONArray explainses = basic.getJSONArray("explains");
//                for (int i = 0; i < explainses.size(); i++) {
//                    String explains = explainses.getString(i);
//                    String convert2aB = TranslateSupport.convert2aB(explains);
//                    values.add(convert2aB);
//                }
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return values;
    }


    public static String truncate(String q) {
        if (q == null) {
            return null;
        }
        int len = q.length();
        String result;
        return len <= 20 ? q : (q.substring(0, 10) + len + q.substring(len - 10, len));
    }

       /**
     * 生成加密字段
     */
    public static String getDigest(String string) {
        if (string == null) {
            return null;
        }
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        byte[] btInput = string.getBytes();
        try {
            MessageDigest mdInst = MessageDigest.getInstance("SHA-256");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
