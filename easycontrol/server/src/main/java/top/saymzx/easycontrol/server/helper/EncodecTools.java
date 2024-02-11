package top.saymzx.easycontrol.server.helper;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class EncodecTools {
  private static ArrayList<String> hevcEncodecList = null;
  private static ArrayList<String> avcEncodecList = null;
  private static ArrayList<String> opusEncodecList = null;
  private static Boolean isSupportOpus = null;
  private static Boolean isSupportH265 = null;
  private static String videoEncoder = null;

  // 获取解码器列表
  private static void getEncodecList() {
    MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
    hevcEncodecList = new ArrayList<>();
    avcEncodecList = new ArrayList<>();
    opusEncodecList = new ArrayList<>();
    Pattern codecPattern = Pattern.compile("(?i)(mtk|qti|qcom|exynos|hisi)");
    for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
      String codecName = mediaCodecInfo.getName();
      if (mediaCodecInfo.isEncoder() && codecPattern.matcher(codecName).find()) {
        if (codecName.toLowerCase().contains("opus")) opusEncodecList.add(codecName);
        else if (codecName.toLowerCase().contains("hevc") || codecName.toLowerCase().contains("h265")) hevcEncodecList.add(codecName);
        else if (codecName.toLowerCase().contains("avc") || codecName.toLowerCase().contains("h264")) avcEncodecList.add(codecName);
      }
    }
  }

  // 获取解码器是否支持
  public static boolean isSupportOpus() {
    if (isSupportOpus != null) return isSupportOpus;
    if (opusEncodecList == null) getEncodecList();
    isSupportOpus = opusEncodecList.size() > 0;
    return isSupportOpus;
  }

  public static boolean isSupportH265() {
    if (isSupportH265 != null) return isSupportH265;
    if (hevcEncodecList == null) getEncodecList();
    isSupportH265 = hevcEncodecList.size() > 0;
    return isSupportH265;
  }

  // 获取视频最优解码器
  public static String getVideoEncoder(boolean h265) {
    if (videoEncoder != null) return videoEncoder;
    if (hevcEncodecList == null || avcEncodecList == null) getEncodecList();
    ArrayList<String> allHardNormalEncodec = h265 ? hevcEncodecList : avcEncodecList;
    ArrayList<String> allHardLowLatencyEncodec = new ArrayList<>();
    for (String codecName : allHardNormalEncodec) if (codecName.contains("low_latency")) allHardLowLatencyEncodec.add(codecName);
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

  // 优选C2解码器
  private static String getC2Encodec(ArrayList<String> allHardEncodec) {
    for (String codecName : allHardEncodec) if (codecName.contains("c2")) return codecName;
    return allHardEncodec.get(0);
  }
}
