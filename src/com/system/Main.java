package com.system;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 启动类
 */
public class Main {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9000);
        System.out.println(LocalDateTime.now() + " 服务启动！");
        //使用线程池处理请求 线程数量
        ExecutorService pool = Executors.newFixedThreadPool(10);
        while (true) {
            try {
                System.out.println("进入 accept 阻塞监听");
                //accept阻塞方法 直到有客户端连接才继续往下走
                Socket connection = serverSocket.accept();
                //提交任务 execute 提交不需要返回值的任务, submit 提交需要返回值的任务 这是个异步操作
                pool.execute(new UserConnection(connection));
            } catch (IOException e) {
                System.out.println(LocalDateTime.now() + " 用户连接断开");
                e.printStackTrace();
            }
        }
    }
}
