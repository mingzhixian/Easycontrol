package top.saymzx.scrcpy.android.helper

import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import top.saymzx.scrcpy.android.appData

class NetHelper {
  private val okhttpClient = OkHttpClient()

  // 网络获取JSON
  fun getJson(url: String, handle: (json: JSONObject?) -> Unit) {
    appData.mainScope.launch {
      withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url)
        try {
          okhttpClient.newCall(request.build()).execute().use { response ->
            withContext(Dispatchers.Main) {
              handle(JSONObject(response.body!!.string()))
            }
          }
        } catch (e: Exception) {
          Log.e("Scrcpy", e.toString())
          withContext(Dispatchers.Main) {
            Toast.makeText(appData.main, "网络错误", Toast.LENGTH_SHORT).show()
            handle(null)
          }
        }
      }
    }
  }

  fun getJson(url: String, value: JSONObject, handle: (json: JSONObject?) -> Unit) {
    appData.mainScope.launch {
      withContext(Dispatchers.IO) {
        val requestBody =
          value.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(requestBody)
        try {
          okhttpClient.newCall(request.build()).execute().use { response ->
            withContext(Dispatchers.Main) {
              handle(JSONObject(response.body!!.string()))
            }
          }
        } catch (e: Exception) {
          Log.e("Scrcpy", e.toString())
          withContext(Dispatchers.Main) {
            Toast.makeText(appData.main, "网络错误", Toast.LENGTH_SHORT).show()
            handle(null)
          }
        }
      }
    }
  }
}