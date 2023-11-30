package top.saymzx.easycontrol.app.entity;

import android.content.SharedPreferences;
import android.util.Pair;

import java.util.UUID;

public final class Setting {
  private final SharedPreferences sharedPreferences;

  private final SharedPreferences.Editor editor;

  public boolean getDefaultIsAudio() {
    return sharedPreferences.getBoolean("defaultIsAudio", true);
  }

  public void setDefaultIsAudio(boolean value) {
    editor.putBoolean("defaultIsAudio", value);
    editor.apply();
  }

  public int getDefaultMaxSize() {
    return sharedPreferences.getInt("defaultMaxSize", 1600);
  }

  public void setDefaultMaxSize(int value) {
    editor.putInt("defaultMaxSize", value);
    editor.apply();
  }

  public int getDefaultMaxFps() {
    return sharedPreferences.getInt("defaultMaxFps", 60);
  }

  public void setDefaultMaxFps(int value) {
    editor.putInt("defaultMaxFps", value);
    editor.apply();
  }

  public int getDefaultMaxVideoBit() {
    return sharedPreferences.getInt("defaultMaxVideoBit", 4);
  }

  public void setDefaultMaxVideoBit(int value) {
    editor.putInt("defaultMaxVideoBit", value);
    editor.apply();
  }

  public boolean getDefaultSetResolution() {
    return sharedPreferences.getBoolean("defaultSetResolution", false);
  }

  public void setDefaultSetResolution(boolean value) {
    editor.putBoolean("defaultSetResolution", value);
    editor.apply();
  }

  public boolean getUseH265() {
    return sharedPreferences.getBoolean("useH265", true);
  }

  public void setUseH265(boolean value) {
    editor.putBoolean("useH265", value);
    editor.apply();
  }

  public boolean getUseTunnel() {
    return sharedPreferences.getBoolean("useTunnel", false);
  }

  public void setUseTunnel(boolean value) {
    editor.putBoolean("useTunnel", value);
    editor.apply();
  }

  public boolean getTurnOffScreen() {
    return sharedPreferences.getBoolean("turnOffScreen", true);
  }

  public void setTurnOffScreen(boolean value) {
    editor.putBoolean("turnOffScreen", value);
    editor.apply();
  }

  public boolean getAutoControlScreen() {
    return sharedPreferences.getBoolean("autoControlScreen", true);
  }

  public void setAutoControlScreen(boolean value) {
    editor.putBoolean("autoControlScreen", value);
    editor.apply();
  }

  public boolean getDefaultFull() {
    return sharedPreferences.getBoolean("defaultFull", false);
  }

  public void setDefaultFull(boolean value) {
    editor.putBoolean("defaultFull", value);
    editor.apply();
  }

  public Pair<Boolean, String> getMasterAudoRotation() {
    return new Pair<>(sharedPreferences.getBoolean("masterAudoRotation", true), "仅在全屏状态下生效，开启后当旋转主控端时会自动旋转页面");
  }

  public void setMasterAudoRotation(boolean value) {
    editor.putBoolean("masterAudoRotation", value);
    editor.apply();
  }

  public Pair<Boolean, String> getSlaveAudoRotation() {
    return new Pair<>(sharedPreferences.getBoolean("slaveAudoRotation", true), "仅在全屏状态下生效，开启后当主控端页面旋转时，会尝试旋转被控端");
  }

  public void setSlaveAudoRotation(boolean value) {
    editor.putBoolean("slaveAudoRotation", value);
    editor.apply();
  }

  public String getDefaultDevice() {
    return sharedPreferences.getString("defaultDevice", "");
  }

  public void setDefaultDevice(String value) {
    editor.putString("defaultDevice", value);
    editor.apply();
  }

  public String getCenterAddress() {
    return sharedPreferences.getString("centerAddress", "");
  }

  public void setCenterAddress(String value) {
    editor.putString("centerAddress", value);
    editor.apply();
  }

  public String getCenterName() {
    return sharedPreferences.getString("centerName", "");
  }

  public void setCenterName(String value) {
    editor.putString("centerName", value);
    editor.apply();
  }

  public String getCenterPassword() {
    return sharedPreferences.getString("centerPassword", "");
  }

  public void setCenterPassword(String value) {
    editor.putString("centerPassword", value);
    editor.apply();
  }

  public int getCenterAdbPort() {
    return sharedPreferences.getInt("centerAdbPort", -1);
  }

  public void setCenterAdbPort(int value) {
    editor.putInt("centerAdbPort", value);
    editor.apply();
  }

  public String getLocalUUID() {
    if (!sharedPreferences.contains("UUID")) {
      editor.putString("UUID", UUID.randomUUID().toString());
      editor.apply();
    }
    return sharedPreferences.getString("UUID", "");
  }

  public Setting(SharedPreferences sharedPreferences) {
    this.sharedPreferences = sharedPreferences;
    this.editor = sharedPreferences.edit();
  }
}
