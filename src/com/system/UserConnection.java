package com.system;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 线程类
 */
public class UserConnection implements Runnable {
    private static final String BLANK = " ";
    //换行符，不能写反了！
    private static final String CRLF = "\r\n";
    private static final String ENCODE = "UTF-8";
    //当任何匹配路径都没有时。
    private static final String default_value = "404 NOT FOUND！\n没有找到你配置的请求！";
    private Socket connection;


    public UserConnection(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {

        //响应头数据
        String contentType = "application/json";
        String body = default_value;
        int contentLength = 0;
        String date = this.getDate();
        String server = "CrazyDragon";
        try (
                InputStream in = connection.getInputStream();
                OutputStream out = connection.getOutputStream()
        ) {
            //这个数字是随便设置的，因为要一次性读取整个请求报文，不能太小。（但是其实这个也很大了）
            byte[] b = new byte[5 * 1024];
            BufferedInputStream input = new BufferedInputStream(in);
            int count = input.read(b);
            String requestMessage = new String(b, 0, count);
            System.out.println("====================请求报文分隔符上界===================");
            System.out.println(requestMessage);
            System.out.println("====================请求报文分隔符下界===================");

            //以第一个 换行符 CRLF 为界限取出 请求头和请求体
            //取出请求头的第一行(请求行) 也就是 : GET / HTTP/1.1
            String requestLine = requestMessage.substring(0, requestMessage.indexOf(CRLF));
            //通过空格分割
            String[] line = requestLine.split("\\s");
            //取出请求方法 考虑大小写
            String method = line[0];
            //取出请求路径
            String path = line[1];
            //判断请求方法
            if ("GET".equals(method)) {
                //这里需要对 get 方式时的请求进行解码，因为一些非 ASCII 码字符会被编码，比如汉字。
                path = URLDecoder.decode(path, ENCODE);
                if ("/".equals(path)) {
                    contentType = "application/json";
                    body = "GET 访问根路径 响应体";
                }
                if ("/favicon.ico".equals(path)) {
                    contentType = "test";
                    body = "C:\\Users\\admin\\Pictures\\Saved Pictures\\微信图片_20211224134546.jpg";
                }
            } else if ("POST".equals(method)) {
                //如果是post方法 取出请求体  不包含换行符
                String bodyRequest = requestMessage.substring(requestMessage.lastIndexOf(CRLF) + 2);
                //因为这个get方式的 参数为空，所以这里必须是 param 在前。
                if ("/".equals(path)) {
                    System.out.println("请求体数据为:" + bodyRequest);
                    contentType = "application/json";
                    body = "post 访问根路径 响应体 携带参数为" + bodyRequest;
                }
            }
            File file = null;
            //响应体
            if ("application/json".equals(contentType)) {
                //如果是 json 数据，否则就是 文件类数据（图片、文档或其它文件）
                //响应体的字节数，注意是字节数！
                contentLength = body.getBytes().length;
            } else {
                file = new File(body);
                contentLength = (int) file.length();
                body = null;
            }

            //响应头
            StringBuilder responseHeader = new StringBuilder();
            //状态 必须要有 OK也是可有可无, HTTP/1.1 和 200 必须要有
            responseHeader.append("HTTP/1.1").append(BLANK).append(200).append(BLANK).append("OK");
            //换行
            responseHeader.append(CRLF);
            //服务器名字 有没有都行
            responseHeader.append("Server:").append(server);
            //换行
            responseHeader.append(CRLF);
            //当前的GMT时间  有没有都行
            responseHeader.append("Date:").append(BLANK).append(date);
            //换行
            responseHeader.append(CRLF);
            //返回类型 有没有都行
            responseHeader.append("Content-Type:").append(BLANK).append(contentType);
            //换行
            responseHeader.append(CRLF);
            //内容长度 有没有都行
            responseHeader.append("Content-Length:").append(BLANK).append(contentLength);
            //两次换行区分响应头和响应体, 如果有多个两次换行, 以第一次作为分割标识, 其余的均为响应体中内容
            responseHeader.append(CRLF);
            responseHeader.append(CRLF);

            //如果 字符串变量 responseBody 不为空，则说明返回值是 json 数据（字符串）
            //否则就是文件类的流了。
            if (body != null) {
                String response = responseHeader.toString() + body;
                out.write(response.getBytes(StandardCharsets.UTF_8));
            } else {
                out.write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
                int hasRead = 0;
                byte[] data = new byte[4 * 1024];
                try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                    while ((hasRead = inputStream.read(data)) != -1) {
                        out.write(data, 0, hasRead);
                    }
                }
            }
            out.flush();   //必要的刷新流操作。
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDate() {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.CHINA);
        // 设置时区为GMT
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }
}