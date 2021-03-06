/*
 * Copyright (c) 2019 JCav <825195983@qq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jcav.bilibilidanmakubackup.utilities;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public final class IOUtilities {
    public static String ILLEGAL_PATH_CHARS = "\\/:?\"<>|";
    public static boolean hasIllegalChar(String path){
        for(int index = 0;index < path.length();index++){
            switch (path.charAt(index)){
                case '\\':
                case '/':
                case ':':
                case '?':
                case '"':
                case '<':
                case '>':
                case '|':
                    return true;
            }
        }
        return false;
    }

    /**
     * @param url
     * @return the host of the url, an empty string if matching is failed.
     */
    public static String getHost(String url){
        final Pattern pat = Pattern.compile("http(s|)://(.*?)/");
        Matcher mat = pat.matcher(url);
        if(mat.find()){
            return mat.group(2);
        }else{
            return "";
        }
    }

    public static HttpsURLConnection createHttpsConnection(String url, Proxy proxy) throws IOException {
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(
                    null,
                    new TrustManager[] { new TrustAnyTrustManager() },
                    new java.security.SecureRandom()
            );
        } catch (Exception e){}

        HttpsURLConnection con = null;
        if(proxy != null) con = (HttpsURLConnection) new URL(url).openConnection(proxy);
        else con =  (HttpsURLConnection) new URL(url).openConnection();

        con.setSSLSocketFactory(sc.getSocketFactory());
        con.setRequestProperty("Accept", "*/*");
        con.setRequestProperty("Accept-Encoding", "gzip, deflate");
        con.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        con.setRequestProperty("Host", getHost(url));

        return con;
    }

    public static String downloadAsString(HttpURLConnection cn) throws IOException{
        return downloadAsString(cn, 0);
    }

    public static String downloadAsString(HttpURLConnection cn, int timeout) throws IOException {
        cn.setConnectTimeout(timeout);
        cn.setReadTimeout(timeout);

        if(cn.getResponseCode() != 200) {
            return null;
        }
        if(!cn.getDoInput()) return null;

        InputStream in = cn.getInputStream();
        in = new BufferedInputStream(in);

        String encoding = cn.getHeaderField("Content-Encoding");
        if(encoding != null){
            if(encoding.indexOf("gzip") != -1){
                in = new GZIPInputStream(in);
            }else if(encoding.indexOf("deflate") != -1){
                in = new InflaterInputStream(in, new Inflater(true));
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(
                cn.getContentLength() <= 0 ? 1024 : cn.getContentLength()
        );

        byte[] buf = new byte[1024];
        int c = 0;
        while((c = in.read(buf)) != -1){
            out.write(buf, 0, c);
        }
        return out.toString("utf-8");
    }

    /**
     *
     * @param file
     * @return null if something is wrong
     */
    public static String readAllString(File file){
        String str = null;
        try {
            str = new String(
                    Files.readAllBytes(file.toPath()),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
        }
        return str;
    }

    /**
     * @param file
     * @return null if failed.
     */
    public static PrintWriter createPrintWriter(File file){
        PrintWriter pw = null;
        try{
            pw = new PrintWriter(
                    new OutputStreamWriter(
                            new BufferedOutputStream(
                                    new FileOutputStream(file)
                            ),
                            "utf-8"
                    ),
                    true
            );
        } catch (IOException e) {
            pw = null;
        }
        return pw;
    }

    public static boolean saveToFile(String str, File file){
        if(file.exists()) file.delete();
        try {
            Files.write(
                    file.toPath(),
                    str.getBytes("utf-8"),
                    StandardOpenOption.CREATE
            );
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}



