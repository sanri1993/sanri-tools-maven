package com.sanri.app.translate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;
import sanri.utils.HttpUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 百度英语翻译
 */
public class BaiduEnglishTranslate implements Translate ,EnglishTranslate{
    static final String baiduUrl = "http://api.fanyi.baidu.com/api/trans/vip/translate";
    static final String appId = "20181123000238271";
    static final String salt = "sanri";

    private Log logger = LogFactory.getLog(getClass());

    @Override
    public void doTranslate(TranslateCharSequence translateCharSequence, TranslateChain translateChain) {
        Set<String> needTranslateWords = translateCharSequence.getNeedTranslateWords();
        if(CollectionUtils.isEmpty(needTranslateWords)){return ;}

        for (String needTranslateWord : needTranslateWords) {
            //因为百度修改 qps = 1 ，需要降低使用频率 ；
            try {Thread.sleep(1000);} catch (InterruptedException e) {}

            translateWord(needTranslateWord,translateCharSequence);
        }

        //添加直译结果
        Set<String> results = directTranslate(translateCharSequence.getOriginSequence().toString());
        for (String result : results) {
            translateCharSequence.addDirectTranslate(result);
        }

        translateChain.doTranslate(translateCharSequence,translateChain);
    }

    /**
     * 单个词的翻译结果
     * @param needTranslateWord
     * @param translateCharSequence
     */
    private void translateWord(String needTranslateWord,TranslateCharSequence translateCharSequence) {
        Set<String> translate = translate(needTranslateWord);
        if(!CollectionUtils.isEmpty(translate)){
            for (String result : translate) {
                translateCharSequence.addTranslate(needTranslateWord,result);
            }
        }else{
            logger.error("百度翻译失败,使用原词代替");
            translateCharSequence.addTranslate(needTranslateWord,needTranslateWord);
        }
    }

    /**
     * 调用百度翻译
     * @param needTranslateWord
     * @return
     */
    private Set<String> translate(String needTranslateWord) {
        Set<String> results = new HashSet<String>();

        Map<String, String> params = new HashMap<String, String>();
        params.put("from", "zh");
        params.put("to", "en");
        params.put("q", needTranslateWord);
        params.put("salt", salt);
        params.put("appid", appId);

        String  sign = DigestUtils.md5Hex(appId + needTranslateWord + salt + "PoT5TnMl_4pVIhosG_Fk");
        params.put("sign", sign);

        try {
            JSONObject jsonObject = HttpUtil.getJSON(baiduUrl, params);
            JSONArray transResult = jsonObject.getJSONArray("trans_result");
            if (transResult != null && !transResult.isEmpty()) {
                for (int i = 0; i < transResult.size(); i++) {
                    JSONObject currData = transResult.getJSONObject(i);
                    String dst = currData.getString("dst");
                    String[] split = dst.split(" ");
                    StringBuffer stringBuffer = new StringBuffer(StringUtils.uncapitalize(split[0]));
                    for (int j = 1; j < split.length; j++) {
                        stringBuffer.append(StringUtils.capitalize(split[j]));
                    }

                    results.add(stringBuffer.toString());
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return  results;
    }

    @Override
    public Set<String> directTranslate(String source) {
        Set<String> translate = translate(source);
        return translate;
    }
}
