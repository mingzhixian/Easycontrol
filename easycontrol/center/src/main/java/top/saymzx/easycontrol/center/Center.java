package top.saymzx.easycontrol.center;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Center {
  // 线程池
  private static final ExecutorService executor = new ThreadPoolExecutor(5, 20, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());

  // 用户列表，其实应该用数据库存储的，我懒，本程序只是一种简单示例，也没有什么安全、高并发的考虑
  public static final HashMap<String, User> users = new HashMap<>();

  public static void main(String[] args) throws IOException {
    // 启动检查线程
    executor.execute(Center::checkAddress);
    // 启动Web服务器
    HttpServer httpServer = HttpServer.create(new InetSocketAddress(8866), 0);
    httpServer.createContext("/api/", new MyHttpHandler());
    httpServer.setExecutor(executor);
    httpServer.start();
  }

  private static void checkAddress() {
    while (true) {
      try {
        Thread.sleep(60 * 15);
      } catch (InterruptedException e) {
        return;
      }
      long now = System.currentTimeMillis();
      for (User user : users.values()) user.devices.removeIf(address -> now - address.getLastPostTime() > 1000 * 60 * 90);
    }
  }
}