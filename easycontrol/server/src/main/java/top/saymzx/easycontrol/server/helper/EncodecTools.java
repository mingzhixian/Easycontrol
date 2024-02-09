package top.saymzx.easycontrol.server.helper;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import java.util.ArrayList;

public class EncodecTools {
  private static ArrayList<MediaCodecInfo> encodecList = null;
  private static Boolean isSupportOpus = null;
  private static Boolean isSupportH265 = null;
  private static String videoEncoder = null;

  // 获取解码器列表
  private static void getDecodecList() {
    encodecList = new ArrayList<>();
    MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
    for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) if (mediaCodecInfo.isEncoder()) encodecList.add(mediaCodecInfo);
  }

  // 获取解码器是否支持
  public static boolean isSupportOpus() {
    if (isSupportOpus != null) return isSupportOpus;
    if (encodecList == null) getDecodecList();
    for (MediaCodecInfo mediaCodecInfo : encodecList) {
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
    if (encodecList == null) getDecodecList();
    for (MediaCodecInfo mediaCodecInfo : encodecList) {
      String codecName = mediaCodecInfo.getName();
      // 是h265解码器
      if (codecName.contains("hevc") || codecName.contains("h265")) {
        // 优选硬件解码器
        if (isHardEncodec(codecName)) {
          isSupportH265 = true;
          return isSupportH265;
        }
      }
    }
    isSupportH265 = false;
    return isSupportH265;
  }

  // 获取视频最优解码器
  public static String getVideoEncoder(boolean h265) {
    if (videoEncoder != null) return videoEncoder;
    if (encodecList == null) getDecodecList();
    ArrayList<String> allHardNormalEncodec = new ArrayList<>();
    ArrayList<String> allHardLowLatencyEncodec = new ArrayList<>();
    for (MediaCodecInfo mediaCodecInfo : encodecList) {
      String codecName = mediaCodecInfo.getName();
      // 查找对应解码器
      if (codecName.contains(h265 ? "hevc" : "avc") || codecName.contains(h265 ? "h265" : "h264")) {
        // 优选硬件解码器
        if (isHardEncodec(codecName)) {
          // 优选低延迟
          if (codecName.contains("low_latency")) allHardLowLatencyEncodec.add(codecName);
          else allHardNormalEncodec.add(codecName);
        }
      }
    }
    // 存在低延迟解码器
    if (allHardLowLatencyEncodec.size() > 0) {
      videoEncoder = getC2Encodec(allHardLowLatencyEncodec);
      return videoEncoder;
    }
    // 选择正常解码器
    if (allHardNormalEncodec.size() > 0) {
      videoEncoder = getC2Encodec(allHardNormalEncodec);
      return videoEncoder;
    }
    return "";
  }

  // 检查是否为硬件解码器
  private static boolean isHardEncodec(String codecName) {
    return (codecName.contains("mtk") || codecName.contains("qti") || codecName.contains("qcom") || codecName.contains("exynos") || codecName.contains("hisi"));
  }

  // 优选C2解码器
  private static String getC2Encodec(ArrayList<String> allHardEncodec) {
    for (String codecName : allHardEncodec) if (codecName.contains("c2")) return codecName;
    return allHardEncodec.get(0);
  }
}
