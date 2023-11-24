package top.saymzx.easycontrol.app.entity;

import android.content.SharedPreferences;

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

  public int getDefaultVideoBit() {
    return sharedPreferences.getInt("defaultVideoBit", 8);
  }

  public void setDefaultVideoBit(int value) {
    editor.putInt("defaultVideoBit", value);
    editor.apply();
  }

  public boolean getDefaultSetResolution() {
    return sharedPreferences.getBoolean("defaultSetResolution", false);
  }

  public void setDefaultSetResolution(boolean value) {
    editor.putBoolean("defaultSetResolution", value);
    editor.apply();
  }

  public boolean getDefaultH265() {
    return sharedPreferences.getBoolean("defaultH265", true);
  }

  public void setDefaultH265(boolean value) {
    editor.putBoolean("defaultH265", value);
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

  public boolean getAudoRotation() {
    return sharedPreferences.getBoolean("audoRotation", true);
  }

  public void setAudoRotation(boolean value) {
    editor.putBoolean("audoRotation", value);
    editor.apply();
  }

  public boolean getSendMoreOk() {
    return sharedPreferences.getBoolean("sendMoreOk", true);
  }

  public void setSendMoreOk(boolean value) {
    editor.putBoolean("sendMoreOk", value);
    editor.apply();
  }

  public boolean getMultipleAdb() {
    return sharedPreferences.getBoolean("multipleAdb", true);
  }

  public void setMultipleAdb(boolean value) {
    editor.putBoolean("multipleAdb", value);
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
