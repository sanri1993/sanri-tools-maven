package com.sanri.app.translate;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * 英语翻译
 */
public interface EnglishTranslate  {

    /**
     * 直译后拼接成驼峰式
     * @param source
     * @return
     */
   public Set<String> directTranslate(String source);
}
