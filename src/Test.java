import Util.HttpClientUtil;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.activerecord.CaseInsensitiveContainerFactory;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.c3p0.C3p0Plugin;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Phn on 2017/3/23.
 */
public class Test {
    public static void main(String[] args) throws Exception {

        initWsDb();
        String main_url = "http://192.168.2.229/newkc/akcjj0.asp?xqh=20162";//主页
        String course_url = "http://192.168.2.229/newkc/kccx.asp?flag=kkdw";//课程列表url
        String unit_url = "http://192.168.2.229/newkc/akechengdw.asp";//单位列表url
        String detail_url = "http://192.168.2.229/newkc/kcxkrs.asp?ykch=" ;//详细信息
//        Connection con = Jsoup.connect(main_url);
//        Connection.Response response = con.execute();
//        Map<String, String> cookie = response.cookies();
//        Connection conSearch = Jsoup.connect(unit_url);
//        Iterator<Map.Entry<String, String>> iterCookie = cookie.entrySet().iterator();
//        while(iterCookie.hasNext()){
//            Map.Entry<String, String> entry = iterCookie.next();
//            conSearch.cookie(entry.getKey(), entry.getValue());
//        }
//        Document doc = conSearch.get();
//        Elements units=doc.getElementsByTag("option");

        Map<String, String> cookie = get(main_url);
        List<String> course_number=new ArrayList<>();
        Document doc=Jsoup.parse(get(unit_url,cookie,"gbk"));
        Elements units=doc.getElementsByTag("option");
        List<String> unitList=new ArrayList<String>();
        for(Element unit:units){
            String unitStr=unit.attr("value").trim();
            unitList.add(unitStr);
            Map<String, String> params =new HashMap<String, String>() ;
            System.out.println("==================正在解析"+unitStr+"的排课=======================");
            params.put("bh",unit.attr("value").trim());
            params.put("SUBMIT","查询");
            String data=post(course_url,params,cookie,"gbk");
            Document courseDoc=Jsoup.parse(data);
            Elements trs=courseDoc.getElementsByTag("tr");
            trs.remove(0);
            for(Element tr:trs){
                Elements tds=tr.getElementsByTag("td");
                if(tds.size()>9){
                    Element idTd=tds.get(1);
                    Element nameTd=tds.get(2);
                    Element teacherTd=tds.get(9);
                   // System.out.println(idTd.text()+"#"+nameTd.text()+"#"+teacherTd.text());
                    course_number.add(idTd.text());
                }


            }
        }
//        for(int i=0;i<course_number.size();i++){
//            System.out.println(i+":"+course_number.get(i));
//        }



        System.out.println("==================获取全部课程号完成=======================");
        Thread.sleep(2000);
        System.out.println("==================开始获取全部学生信息=======================");

        for (int i=0;i<course_number.size();i++){
            String id=course_number.get(i);
            String detail=detail_url+id;
            String data=get(detail,cookie,"gbk");
            Document cdetailDoc=Jsoup.parse(data);
            Element table1=cdetailDoc.getElementsByTag("table").first();
            Element table2=cdetailDoc.getElementsByTag("table").get(1);
            Elements trs1=table1.getElementsByTag("tr");
            Elements trs2=table2.getElementsByTag("tr");

            //保存课程信息
            Elements tds=trs1.first().getElementsByTag("td");
            String xqh=tds.get(1).text();
            String course_num=tds.get(3).text();
            String name=tds.get(5).text();
            String type=tds.get(7).text();
            Elements tds2=trs1.get(1).getElementsByTag("td");
            String teather=tds2.get(1).text();
            String khfs=tds2.get(3).text();
            String xf=tds2.get(5).text();
            String count=tds2.get(7).text();
            Record course=new Record();
            course.set("xqh",xqh);
            course.set("course_num",course_num);
            course.set("name",name);
            course.set("type",type);
            course.set("teacher",teather);
            course.set("khfs",khfs);
            course.set("xf",xf);
            course.set("count",count);
            Db.save("course",course);
            System.out.println(xqh+"#"+course_num+"#"+name+"#"+type+"#"+teather+"#"+khfs+"#"+xf+"#"+count+"#入库成功");

            //保存学生信息
            trs2.remove(0);
            for(Element tr:trs2){
                Elements stds=tr.getElementsByTag("td");
                String stuno=stds.get(1).text();
                String sname=stds.get(2).text();
                String sex=stds.get(3).text();
                String grade=stds.get(4).text();
                Record stu=new Record();
                stu.set("stuno",stuno);
                stu.set("name",sname);
                stu.set("sex",sex);
                stu.set("grade",grade);
                stu.set("coursenum",course_num);
                Db.save("student",stu);
                System.out.println(stuno+"#"+sname+"#"+sex+"#"+grade+"#入库成功");
            }



            }

        System.out.println("==================全部信息入库成功=======================");



    }

    public static String post(String url, Map<String, String> params,Map<String,String> cookie,String charset) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        String body = null;
        try{
            HttpPost httpost = new HttpPost(url);
            List<NameValuePair> nvps = new ArrayList <NameValuePair>();
            Set<String> keySet = params.keySet();
            for(String key : keySet) {
                nvps.add(new BasicNameValuePair(key, params.get(key)));
            }
            httpost.setEntity(new UrlEncodedFormEntity(nvps, "gb2312"));
            httpost.setHeader("Cookie",getCookies(cookie));
            HttpResponse response = httpclient.execute(httpost);
            HttpEntity entity = response.getEntity();
            body = EntityUtils.toString(entity,charset);
            httpclient.getConnectionManager().shutdown();
        }catch (Exception e){
            e.printStackTrace();
        }


        return body;
    }

    public static Map<String,String>  get(String url) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        Map<String,String> cookieMap=new HashMap<>();
        try{
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpget);
            httpclient.getConnectionManager().shutdown();
            CookieStore cookieStore=httpclient.getCookieStore();
            List<Cookie> clist=cookieStore.getCookies();
            for(Cookie c:clist){
                cookieMap.put(c.getName(),c.getValue());
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return  cookieMap;
    }

    public static String get(String url,Map<String,String> cookie,String charset) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        String body = null;
        try{
            HttpGet httpget = new HttpGet(url);
            httpget.setHeader("Cookie",getCookies(cookie));
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            body = EntityUtils.toString(entity,charset);
            httpclient.getConnectionManager().shutdown();
        }catch (Exception e){
            e.printStackTrace();
        }


        return body;
    }


    public static String getCookies( Map<String, String> cookie) {
        StringBuilder sb = new StringBuilder();

        Iterator<Map.Entry<String, String>> iterCookie = cookie.entrySet().iterator();
        while(iterCookie.hasNext()){
            Map.Entry<String, String> entry = iterCookie.next();
            sb.append(entry.getKey() + "=" + entry.getValue() + ";");
        }
        return sb.toString();
    }

    //初始化本地数据库

    public static void initWsDb(){

        C3p0Plugin c3p0Plugin = new C3p0Plugin("jdbc:mysql:********",
                "*****","*******".trim());
        c3p0Plugin.setDriverClass("com.mysql.jdbc.Driver");
        c3p0Plugin.setMaxPoolSize(1000);
        c3p0Plugin.start();
        ActiveRecordPlugin arp = new ActiveRecordPlugin("databases",c3p0Plugin);

        // 配置属性名(字段名)大小写不敏感容器工厂
        arp.setContainerFactory(new CaseInsensitiveContainerFactory(true));
        arp.start();
        System.out.println("本地数据库初始化成功.............................");

    }



}
