package com.qiwenshare.common.util;

import cn.hutool.http.HttpUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ICOUtil {
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
            Pattern p1 = Pattern.compile("rel=[^<>]+?href=[^<>]+?\\.ico");
            Matcher m1 = p1.matcher(body);
            if (m1.find()) {
                int i = navUrl.indexOf("/", 8);//获取网址 拼接 favicon.ico
                if (i > 0) {
                    navUrl = navUrl.substring(0, i);
                }


                String res = body.substring(m1.start(), m1.end());
                String icoUrl = res.split("href=")[1];
                if (icoUrl.startsWith("\"")) {
                    icoUrl = icoUrl.substring(1);
                }
                if (icoUrl.startsWith("/")) {
                    icoUrl = icoUrl.substring(1);
                }
                if (navUrl.endsWith("/")) {
                    return navUrl + icoUrl;
                } else {
                    return navUrl + "/" + icoUrl;
                }
            } else {
                Pattern p2 = Pattern.compile("rel=[^<>]+?href=[^<>]+?\\.svg");
                Matcher m2 = p2.matcher(body);
                if (m2.find()) {
                    int i = navUrl.indexOf("/", 8);//获取网址 拼接 favicon.ico
                    if (i > 0) {
                        navUrl = navUrl.substring(0, i);
                    }


                    String res = body.substring(m2.start(), m2.end());
                    String icoUrl = res.split("href=")[1];
                    if (icoUrl.startsWith("\"")) {
                        icoUrl = icoUrl.substring(1);
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
            }


            //说明没有指定 走拼接逻辑
            int i = navUrl.indexOf("/", 8);//获取网址 拼接 favicon.ico
            if (i > 0) {
                navUrl = navUrl.substring(0, i);
            }

            if (navUrl.endsWith("/")) {
                return navUrl + "favicon.ico";
            } else {
                return navUrl + "/favicon.ico";
            }
        }
    }
}
