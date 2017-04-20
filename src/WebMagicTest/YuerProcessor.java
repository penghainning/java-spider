package WebMagicTest;

import us.codecraft.webmagic.*;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.JsonPathSelector;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Phn on 2017/4/17.
 */
public class YuerProcessor implements PageProcessor {

    //列表页的url
    public static final String URL_LIST = "http://www\\.yuerzaixian\\.com/Item/list\\.asp\\?id=\\d+&page=\\d+";
    //新闻详细页的url
    public static final String URL_DEATIL = "http://www.yuerzaixian.com/html/news/myxw/\\d+\\.html";

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private Site site = Site
            .me()
            .setCharset("utf-8")
            .setRetryTimes(3)
            .setSleepTime(1000);

    @Override
    public void process(Page page) {
        //列表页
        if (page.getUrl().regex(URL_LIST).match()) {
            System.out.println("match");
            page.addTargetRequests(page.getHtml().xpath("//div[@class=\"xinxi\"]").links().regex(URL_DEATIL).all());//加入详细页
            page.addTargetRequests(page.getHtml().links().regex(URL_LIST).all());//加入所有符合列表页的Url
            //文章页
        } else {
            page.putField("title", page.getHtml().xpath("//div[@class='biaoti']/h1").regex(">(.*)</h1>"));//匹配标题
           // page.putField("content", page.getHtml().xpath("//div[@id='MyContent']"));
            page.putField("date", page.getHtml().xpath("//div[@class='biaoti']/p").regex("(\\d{4}\\/\\d{1,2}\\/\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2})"));//匹配时间
        }

    }

    @Override
    public Site getSite() {
        return site;
    }


    public static void main(String[] args) {
        SpiderListener myListener=new SpiderListener() {
            @Override
            public void onSuccess(Request request) {
                System.out.println("myListener:success!");
            }

            @Override
            public void onError(Request request) {
                System.out.println("myListener:error!");
            }
        };
        List<SpiderListener> listeners=new ArrayList<>();
        listeners.add(myListener);
        Spider.create(new YuerProcessor()).addUrl("http://www.yuerzaixian.com/Item/list.asp?id=1234&page=1").setSpiderListeners(listeners)
                .run();
    }
}
