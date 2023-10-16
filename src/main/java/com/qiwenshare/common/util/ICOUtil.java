package com.qiwenshare.common.util;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ICOUtil {

    public static void main(String[] args) {
        System.out.println(findIco("https://y.qq.com/"));

    }
    /**
     * 从互联网获取ico
     * @param navUrl navUrl
     * @return 路径
     */
    public static String findIco(String navUrl) {
//        String body = HttpUtil.createGet(navUrl).execute().toString();
        String body = "";
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
            body = HttpsUtils.doGetString(navUrl, header);
        } catch (Exception e) {
            body = HttpUtil.createGet(navUrl).execute().toString();
        }


        Pattern p = Pattern.compile("(https?:)?//[^<>]+?\\.ico");
        Matcher m = p.matcher(body);
        boolean isFind = m.find();
        if (isFind) {
            String res = body.substring(m.start(), m.end());
            if (res.startsWith("//")) {
                if (navUrl.startsWith("https")) {
                    return "https:" + res;
                } else {
                    return "http:" + res;
                }
            } else {
                return res;
            }
        } else {
            Pattern p1 = Pattern.compile("rel=[^<>]+?href=[^<>]+?\\.(ico|svg|png|jpg)");
            Matcher m1 = p1.matcher(body);
            if (m1.find()) {

                navUrl = getPreUrl(navUrl);


                String res = body.substring(m1.start(), m1.end());
                String icoUrl = res.split("href=")[1];

                if (icoUrl.startsWith("\"")) {
                    icoUrl = icoUrl.substring(1);
                }
                if (icoUrl.startsWith("https")||icoUrl.startsWith("http")) {
                    return icoUrl;
                }
                if (icoUrl.startsWith("/")) {
                    icoUrl = icoUrl.substring(1);
                }
                if (navUrl.endsWith("/")) {
                    return navUrl + icoUrl;
                } else {
                    return navUrl + "/" + icoUrl;
                }
            }

            //说明没有指定 走拼接逻辑
            navUrl = getPreUrl(navUrl);

            if (navUrl.endsWith("/")) {
                return navUrl + "favicon.ico";
            } else {
                return navUrl + "/favicon.ico";
            }
        }
    }

    public static String getPreUrl(String navUrl) {
        String res = navUrl;
        int i = navUrl.indexOf("/#/", 8);
        if (i > 0) {
            res = navUrl.substring(0, i);
        } else {
            int j = navUrl.indexOf("/", 8);//获取网址 拼接 favicon.ico
            if (j > 0) {
                res = navUrl.substring(0, j);
            }
        }
        return res;
    }
}
