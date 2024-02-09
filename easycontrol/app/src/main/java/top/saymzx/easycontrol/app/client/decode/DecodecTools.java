package top.saymzx.easycontrol.app.client.decode;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import java.util.ArrayList;

public class DecodecTools {
  private static ArrayList<MediaCodecInfo> decodecList = null;
  private static Boolean isSupportOpus = null;
  private static Boolean isSupportH265 = null;
  private static String videoDecoder = null;

  // 获取解码器列表
  private static void getDecodecList() {
    decodecList = new ArrayList<>();
    MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
    for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) if (!mediaCodecInfo.isEncoder()) decodecList.add(mediaCodecInfo);
  }

  // 获取解码器是否支持
  public static boolean isSupportOpus() {
    if (isSupportOpus != null) return isSupportOpus;
    if (decodecList == null) getDecodecList();
    for (MediaCodecInfo mediaCodecInfo : decodecList) {
      if (mediaCodecInfo.getName().contains("opus")) {
        isSupportOpus = true;
        return isSupportOpus;
      }
    }
    isSupportOpus = false;
    return isSupportOpus;
  }

  public static boolean isSupportH265() {
    if (isSupportH265 != null) return isSupportH265;
    if (decodecList == null) getDecodecList();
    for (MediaCodecInfo mediaCodecInfo : decodecList) {
      String codecName = mediaCodecInfo.getName();
      // 是h265解码器
      if (codecName.contains("hevc") || codecName.contains("h265")) {
        // 优选硬件解码器
        if (isHardDecodec(codecName)) {
          isSupportH265 = true;
          return isSupportH265;
        }
      }
    }
    isSupportH265 = false;
    return isSupportH265;
  }

  // 获取视频最优解码器
  public static String getVideoDecoder(boolean h265) {
    if (videoDecoder != null) return videoDecoder;
    if (decodecList == null) getDecodecList();
    ArrayList<String> allHardNormalDecodec = new ArrayList<>();
    ArrayList<String> allHardLowLatencyDecodec = new ArrayList<>();
    for (MediaCodecInfo mediaCodecInfo : decodecList) {
      String codecName = mediaCodecInfo.getName();
      // 查找对应解码器
      if (codecName.contains(h265 ? "hevc" : "avc") || codecName.contains(h265 ? "h265" : "h264")) {
        // 优选硬件解码器
        if (isHardDecodec(codecName)) {
          // 优选低延迟
          if (codecName.contains("low_latency")) allHardLowLatencyDecodec.add(codecName);
          else allHardNormalDecodec.add(codecName);
        }
      }
    }
    // 存在低延迟解码器
    if (allHardLowLatencyDecodec.size() > 0) {
      videoDecoder = getC2Decodec(allHardLowLatencyDecodec);
      return videoDecoder;
    }
    // 选择正常解码器
    if (allHardNormalDecodec.size() > 0) {
      videoDecoder = getC2Decodec(allHardNormalDecodec);
      return videoDecoder;
    }
    return "";
  }

  // 检查是否为硬件解码器
  private static boolean isHardDecodec(String codecName) {
    return (codecName.contains("mtk") || codecName.contains("qti") || codecName.contains("qcom") || codecName.contains("exynos") || codecName.contains("hisi"));
  }

  // 优选C2解码器
  private static String getC2Decodec(ArrayList<String> allHardDecodec) {
    for (String codecName : allHardDecodec) if (codecName.contains("c2")) return codecName;
    return allHardDecodec.get(0);
  }
}
