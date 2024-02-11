package top.saymzx.easycontrol.app.client.decode;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class DecodecTools {
  private static ArrayList<String> hevcDecodecList = null;
  private static ArrayList<String> avcDecodecList = null;
  private static ArrayList<String> opusDecodecList = null;
  private static Boolean isSupportOpus = null;
  private static Boolean isSupportH265 = null;
  private static String videoDecoder = null;

  // 获取解码器列表
  private static void getDecodecList() {
    MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
    hevcDecodecList = new ArrayList<>();
    avcDecodecList = new ArrayList<>();
    opusDecodecList = new ArrayList<>();
    Pattern codecPattern = Pattern.compile("(?i)(mtk|qti|qcom|exynos|hisi)");
    for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos()) {
      String codecName = mediaCodecInfo.getName();
      if (!mediaCodecInfo.isEncoder() && codecPattern.matcher(codecName).find()) {
        if (codecName.toLowerCase().contains("opus")) opusDecodecList.add(codecName);
        else if (codecName.toLowerCase().contains("hevc") || codecName.toLowerCase().contains("h265")) hevcDecodecList.add(codecName);
        else if (codecName.toLowerCase().contains("avc") || codecName.toLowerCase().contains("h264")) avcDecodecList.add(codecName);
      }
    }
  }

  // 获取解码器是否支持
  public static boolean isSupportOpus() {
    if (isSupportOpus != null) return isSupportOpus;
    if (opusDecodecList == null) getDecodecList();
    isSupportOpus = opusDecodecList.size() > 0;
    return isSupportOpus;
  }

  public static boolean isSupportH265() {
    if (isSupportH265 != null) return isSupportH265;
    if (hevcDecodecList == null) getDecodecList();
    isSupportH265 = hevcDecodecList.size() > 0;
    return isSupportH265;
  }

  // 获取视频最优解码器
  public static String getVideoDecoder(boolean h265) {
    if (videoDecoder != null) return videoDecoder;
    if (hevcDecodecList == null || avcDecodecList == null) getDecodecList();
    ArrayList<String> allHardNormalDecodec = h265 ? hevcDecodecList : avcDecodecList;
    ArrayList<String> allHardLowLatencyDecodec = new ArrayList<>();
    for (String codecName : allHardNormalDecodec) if (codecName.contains("low_latency")) allHardLowLatencyDecodec.add(codecName);
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

  // 优选C2解码器
  private static String getC2Decodec(ArrayList<String> allHardDecodec) {
    for (String codecName : allHardDecodec) if (codecName.contains("c2")) return codecName;
    return allHardDecodec.get(0);
  }
}
