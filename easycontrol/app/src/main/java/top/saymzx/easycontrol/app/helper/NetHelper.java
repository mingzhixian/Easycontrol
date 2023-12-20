package top.saymzx.easycontrol.app.helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import top.saymzx.easycontrol.app.R;
import top.saymzx.easycontrol.app.entity.AppData;

public class NetHelper {
  public static final int RESPONSE_ERROR = 100;
  public static final int RESPONSE_OK = 101;

  public static JSONObject getJson(String url, JSONObject post) throws IOException, JSONException {
    HttpURLConnection connection = (HttpURLConnection) (new URL(url)).openConnection();
    connection.setConnectTimeout(5000);
    connection.setReadTimeout(5000);

    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setUseCaches(false);

    if (post != null) {
      connection.setRequestMethod("POST");
      try (OutputStream outputStream = connection.getOutputStream()) {
        outputStream.write(post.toString().getBytes(StandardCharsets.UTF_8));
      }
    }

    // 获取响应
    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
      StringBuilder responseBody = new StringBuilder();
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()))
      ) {
        String line;
        while ((line = bufferedReader.readLine()) != null) responseBody.append(line);
      }
      connection.disconnect();
      JSONObject jsonObject = new JSONObject(responseBody.toString());
      if (jsonObject.getInt("status") == RESPONSE_ERROR) throw new IOException(jsonObject.getString("msg"));
      return jsonObject;
    } else {
      connection.disconnect();
      throw new IOException(AppData.main.getString(R.string.error_center_error));
    }
  }
}
