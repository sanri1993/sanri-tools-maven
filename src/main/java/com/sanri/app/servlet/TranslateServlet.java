package com.sanri.app.servlet;

import com.sanri.app.BaseServlet;
import com.sanri.app.translate.*;
import com.sanri.frame.RequestMapping;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 创建人     : sanri
 * 创建时间   : 2018/11/21-19:53
 * 功能       : 命名服务,翻译服务
 */
@RequestMapping("/translate")
public class TranslateServlet extends BaseServlet {
    //不受业务改变的翻译器
   CommonTranslate commonTranslate =  new CommonTranslate();
   BaiduEnglishTranslate baiduEnglishTranslate = new BaiduEnglishTranslate();
   YoudaoEnglishTranslate youdaoEnglishTranslate = new YoudaoEnglishTranslate();
   ParticipleIKTranslate participleIKTranslate = new ParticipleIKTranslate();
   PunctuationTranslate punctuationTranslate = new PunctuationTranslate();
   SymbolTranslate symbolTranslate = new SymbolTranslate();

   Map<String,Translate> translateMap = new HashMap<String, Translate>();
    {
        translateMap.put("youdao",youdaoEnglishTranslate);
        translateMap.put("baidu",baiduEnglishTranslate);
    }

    /**
     * 翻译命名服务
     * @param orginChars
     * @param biz 业务连接
     * @return
     */
    public Set<String> translate(String orginChars,String biz,String [] englishs){
        logger.debug("翻译原词为:"+orginChars);
        TranslateChain translateChain = new TranslateChain();
        translateChain.addTranslate(participleIKTranslate)
                .addTranslate(symbolTranslate)
                .addTranslate(commonTranslate);

        //添加业务翻译
        if(!CommonTranslate.COMMON.equals(biz)){
            translateChain.addTranslate(new BizTranslate(biz));
        }
        //添加英语翻译
        if(ArrayUtils.isNotEmpty(englishs)){
            for (String english : englishs) {
               translateChain.addTranslate(translateMap.get(english));
            }
        }
        translateChain.addTranslate(punctuationTranslate);

        TranslateCharSequence translateCharSequence = new TranslateCharSequence(orginChars);

        translateChain.doTranslate(translateCharSequence,translateChain);

        Set<String> results = translateCharSequence.results();

        return results;
    }

    /**
     * 多列翻译
     * @param words
     * @return
     */
    public List<String> mutiTranslate(String [] words){
        List<String> results = new ArrayList<String>();
        for (String word : words) {
            Set<String> translate = baiduEnglishTranslate.directTranslate(word);
            String result = translate.toArray(new String[]{})[0];
            results.add(result);
        }
        return results;
    }
}
