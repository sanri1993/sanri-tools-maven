package com.sanri.app.jsoup.novel;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sanri.utils.HttpUtil;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 创建人     : sanri
 * 创建时间   : 2018/11/16-21:26
 * 功能       :
 */
public class BiqugeNetSource extends NovelNetSource {
    @Override
    public List<Novel> search(String keyword) throws IOException {
        String sourcePhp = url+"/s.php";

        Map<String,String> params = new HashMap<String, String>();
        params.put("ie","gbk");params.put("s",System.currentTimeMillis()+"");
        params.put("q",keyword);

        List<NameValuePair> nameValuePairs = HttpUtil.transferParam(params);
        HttpEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs, Consts.UTF_8);
        String query = EntityUtils.toString(urlEncodedFormEntity, Consts.UTF_8);

        Document document = Jsoup.connect(sourcePhp+"?"+query)
                .timeout(5000).get();

        return parserSearchDoucment(document);
    }

    /**
     * 解析文档信息
     * @param document
     * @return
     */
    private List<Novel> parserSearchDoucment(Document document) {
        List<Novel> novels = new ArrayList<Novel>();

        Elements bookbox = document.select(".bookbox");
        Iterator<Element> iterator = bookbox.iterator();
        while (iterator.hasNext()){
            Novel novel = new Novel();
            novels.add(novel);

            Element bookboxEl = iterator.next();

            //解析  logo 地址
            Element logoEl = bookboxEl.select(".bookimg>a>img").get(0);
            String logoSrc = logoEl.attr("src");
            novel.setLogo(url+logoSrc);

            //得到 bookinfo 属性
            Element bookInfoEl = bookboxEl.select(".bookinfo").get(0);
            parserBookInfo(novel, bookInfoEl);
        }

        return novels;
    }

    /**
     * 解析书箱信息
     * @param novel
     * @param bookinfoEl
     */

    static  final Pattern lastChapterPattern = Pattern.compile("第\\w+章");
    private void parserBookInfo(Novel novel, Element bookinfoEl) {
        //解析 bookname
        Element booknameEl = bookinfoEl.select("h4.bookname>a").get(0);
        String bookname = booknameEl.text();
        novel.setName(bookname);

        //解析章节地址
        String href = booknameEl.attr("href");
        novel.setChapterUrl(url+href);
        novel.setBookId(href);

        //解析分类
        Elements categoryEl = bookinfoEl.select(".cat");
        String category = categoryEl.text();
        String categoryText = category.replaceFirst("分类：","");
        novel.setCategory(categoryText);

        //解析作者
        Elements authorEl = bookinfoEl.select(".author");
        String author = authorEl.text();
        String authorText = author.replaceFirst("作者：","");
        novel.setAuthor(authorText);

        //解析最新章节及标题信息
        Element lastChapterEl = bookinfoEl.select(".update>a").get(0);
        String lastChapter = lastChapterEl.text();
        Matcher matcher = lastChapterPattern.matcher(lastChapter);
        String chapter = matcher.find() ? matcher.group():"";
        String chapterTitle = StringUtils.trim(lastChapter.substring(chapter.length()));
        novel.setLastChapter(chapter);
        novel.setLastChapterTitle(chapterTitle);

        //解析介绍信息
        Elements pEl = bookinfoEl.select("p");
        if(pEl != null && pEl.size() > 0){
            Element introduceEl = pEl.get(0);
            String introduce = introduceEl.text();
            novel.setIntroduce(introduce);
        }

    }

    @Override
    public List<Chapter> chapterCatalog(Novel novel) throws IOException {
        String chapterUrl = novel.getChapterUrl();
        Document document = Jsoup.connect(chapterUrl).timeout(10000).get();

        Element chapterCatalogEl = document.select(".listmain").get(0);
        return parserChapterCatalog(chapterCatalogEl);
    }

    @Override
    public List<Chapter> newest10Chapter(Novel novel) throws IOException {
        return null;
    }

    /**
     * 解析章节目录
     * @param chapterCatalogEl
     */
    private List<Chapter> parserChapterCatalog(Element chapterCatalogEl) {
        List<Chapter> chapters = new ArrayList<Chapter>();

        Elements chapterEls = chapterCatalogEl.select("a");
        Iterator<Element> iterator = chapterEls.iterator();
        int i=0;//用于排除前 6 章
        while (iterator.hasNext()){
            Element chapterEl = iterator.next();
            if(i++ < 6){
                continue;
            }

            //解析章节,标题,和路径
            String chapterAText = chapterEl.text();
            String chapterUri = chapterEl.attr("href");
            Matcher matcher = lastChapterPattern.matcher(chapterAText);
            String chapterSequence = matcher.find() ? matcher.group():"";
            String chapterTitle =  StringUtils.trim(chapterAText.substring(chapterSequence.length()));

            Chapter chapter = new Chapter();
            chapter.setSequence(chapterSequence);
            chapter.setTitle(chapterTitle);
            chapter.setUrl(url+chapterUri);

            chapters.add(chapter);
        }

        return chapters;
    }

    @Override
    public String content(Novel novel, Chapter chapter) throws IOException {
        String url = chapter.getUrl();
        Document document = Jsoup.connect(url).timeout(1000).get();
        Element contentEl = document.getElementById("content");
        String html = contentEl.html();
        return html;
    }
}
