package com.sanri.app.jsoup.novel;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 创建人     : sanri
 * 创建时间   : 2018/11/22-21:22
 * 功能       : 新笔趣阁实现
 */
public class NewBiqugeNetSource extends NovelNetSource  {
    @Override
    public List<Chapter> chapterCatalog(Novel novel) throws IOException {
        String chapterUrl = novel.getChapterUrl();
        Document document = Jsoup.connect(chapterUrl).timeout(10000).get();

        Element chapterCatalogEl = document.getElementById("list");
        return parserChapterCatalog(chapterCatalogEl);
    }

    @Override
    public List<Chapter> newest10Chapter(Novel novel) throws IOException {
        String chapterUrl = novel.getChapterUrl();
        Document document = Jsoup.connect(chapterUrl).timeout(10000).get();

        Element chapterCatalogEl = document.getElementById("list");

        return null;
    }

    static  final Pattern lastChapterPattern = Pattern.compile("第\\w+章");
    private List<Chapter> parserChapterCatalog(Element chapterCatalogEl) {
        List<Chapter> chapters = new ArrayList<Chapter>();

        Elements chapterEls = chapterCatalogEl.select("a");
        Iterator<Element> iterator = chapterEls.iterator();
        while (iterator.hasNext()){
            Element chapterEl = iterator.next();
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

    @Override
    public List<Novel> search(String keyword) throws IOException {
        String sourcePhp = url+"/search.php?keyword="+ URLEncoder.encode(keyword,"utf-8");
        Document document = Jsoup.connect(sourcePhp)
                .timeout(5000).get();

        return parserSearchDoucment(document);
    }

    /**
     * 搜索文档解析
     * @param document
     * @return
     */
    private List<Novel> parserSearchDoucment(Document document) {
        List<Novel> novels = new ArrayList<Novel>();

        Elements select = document.select(".result-item");
        Iterator<Element> iterator = select.iterator();
        while(iterator.hasNext()){
            Element novelEl = iterator.next();
            Novel novel = new Novel();
            novels.add(novel);

            //获取图片信息logo 信息; 获取小说地址信息
            Element picEl = novelEl.select(".result-game-item-pic").get(0);
            Element aEl = picEl.child(0);
            String href = aEl.attr("href");
            novel.setChapterUrl(href);
            Element imgEl = aEl.child(0);
            novel.setLogo(imgEl.attr("src"));

            //获取小说其它信息
            Element detailEl = novelEl.select(".result-game-item-detail").get(0);
            String title = detailEl.child(0).child(0).child(0).text();
            String introduce = detailEl.child(1).text();
            novel.setName(title);
            novel.setIntroduce(introduce);

            Element infoEl = detailEl.child(2);
            String author = infoEl.child(0).child(1).text();
            String category = infoEl.child(1).child(1).text();
            String lastUpdate = infoEl.child(2).child(1).text();
            String lastChapterTitle = infoEl.child(3).child(1).text();
            String lastChapter = infoEl.child(3).child(1).attr("href");
            novel.setAuthor(author);
            novel.setCategory(category);
            novel.setLastChapterTitle(lastChapterTitle);
            novel.setLastChapter(lastChapter);
            novel.setLastUpdateTime(lastUpdate);
        }

        return novels;
    }
}
