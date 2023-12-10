package top.saymzx.easycontrol.center;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MyHttpHandler implements HttpHandler {
  private static final Double VERSION = 4.0;
  private static final int POST_DEVICE = 1;
  private static final int GET_DEVICE = 2;

  private static final int RESPONSE_ERROR = 100;
  private static final int RESPONSE_OK = 101;

  @Override
  public void handle(HttpExchange httpExchange) {
    JSONObject response = new JSONObject();
    try {
      JSONObject params = getRequestParam(httpExchange);
      handleParam(params, response);
    } catch (IOException ignored) {
    }
    try {
      writeResponse(httpExchange, response);
    } catch (IOException ignored) {
    }
  }

  private static void handleParam(JSONObject params, JSONObject response) {
    // 版本不符
    double clientVersion = params.getDouble("version");
    if (clientVersion < VERSION.intValue() || clientVersion > VERSION.intValue() + 1) {
      response.put("status", RESPONSE_ERROR);
      response.put("msg", "版本不符，Center版本为" + VERSION);
      return;
    }
    // 获取用户
    User user = getUser(params.getString("easycontrol_one_one"), params.getString("easycontrol_two_two"));
    // 用户不存在
    if (user == null) {
      response.put("status", RESPONSE_ERROR);
      response.put("msg", "密码错误或用户已存在");
      return;
    }
    // 分发处理
    response.put("status", RESPONSE_OK);
    response.put("msg", "成功");
    switch (params.getInt("handle")) {
      case POST_DEVICE: {
        postDevice(user, params, response);
        break;
      }
      case GET_DEVICE: {
        getDevice(user, params, response);
        break;
      }
    }
  }

  // 获取请求参数
  private static JSONObject getRequestParam(HttpExchange httpExchange) throws IOException {
    StringBuilder paramStr = new StringBuilder();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8));
    String line;
    while ((line = bufferedReader.readLine()) != null) paramStr.append(line);
    return new JSONObject(paramStr.toString());
  }

  private static void postDevice(User user, JSONObject params, JSONObject response) {
    String uuid = params.getString("uuid");
    int adbPort = params.getInt("adbPort");
    String ipv4 = params.getString("ipv4");
    String ipv6 = params.getString("ipv6");
    User.Device postDevice = user.getDevice(uuid);
    if (postDevice == null) {
      postDevice = new User.Device(uuid);
      user.devices.add(postDevice);
    }
    postDevice.update(ipv4, ipv6, adbPort, System.currentTimeMillis());
    System.out.print("Post Device,Name=" + user.name + ",Password=" + user.password + ",UUID=" + uuid + "\n");
  }

  private static void getDevice(User user, JSONObject params, JSONObject response) {
    JSONArray deviceArray = new JSONArray();
    for (User.Device device : user.devices) deviceArray.put(device.toJson());
    response.put("deviceArray", deviceArray);
    System.out.print("Get Device,Name=" + user.name + ",Password=" + user.password + "\n");
  }

  private static User getUser(String name, String password) {
    User user = Center.users.get(name);
    // 没有则创建一个
    if (user == null) {
      user = new User(name, password);
      Center.users.put(name, user);
      System.out.print("New User,Name=" + user.name + ",Password=" + user.password + "\n");
    } else if (!Objects.equals(user.password, password)) user = null;
    return user;
  }

  private static void writeResponse(HttpExchange httpExchange, JSONObject response) throws IOException {
    httpExchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

    byte[] responseContentByte = response.toString().getBytes(StandardCharsets.UTF_8);
    httpExchange.sendResponseHeaders(200, responseContentByte.length);

    OutputStream out = httpExchange.getResponseBody();
    out.write(responseContentByte);
    out.flush();
    out.close();
  }
}
