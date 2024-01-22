package top.saymzx.easycontrol.app.entity;

import android.content.SharedPreferences;

import java.util.UUID;

public final class Setting {
  private final SharedPreferences sharedPreferences;

  private final SharedPreferences.Editor editor;

  public boolean getIsActive() {
    return sharedPreferences.getBoolean("isActive", false);
  }

  public void setIsActive(boolean value) {
    editor.putBoolean("isActive", value);
    editor.apply();
  }

  public String getActiveKey() {
    return sharedPreferences.getString("activeKey", "");
  }

  public void setActiveKey(String value) {
    editor.putString("activeKey", value);
    editor.apply();
  }

  public String getDefaultLocale() {
    return sharedPreferences.getString("defaultLocale", "");
  }

  public void setDefaultLocale(String value) {
    editor.putString("defaultLocale", value);
    editor.apply();
  }

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

  public boolean getDefaultUseH265() {
    return sharedPreferences.getBoolean("defaultUseH265", true);
  }

  public void setDefaultUseH265(boolean value) {
    editor.putBoolean("defaultUseH265", value);
    editor.apply();
  }

  public boolean getDefaultUseOpus() {
    return sharedPreferences.getBoolean("defaultUseOpus", true);
  }

  public void setDefaultUseOpus(boolean value) {
    editor.putBoolean("defaultUseOpus", value);
    editor.apply();
  }

  public boolean getDefaultFull() {
    return sharedPreferences.getBoolean("defaultFull", false);
  }

  public void setDefaultFull(boolean value) {
    editor.putBoolean("defaultFull", value);
    editor.apply();
  }

  public boolean getAutoBackOnStartDefault() {
    return sharedPreferences.getBoolean("autoBackOnStartDefault", false);
  }

  public void setAutoBackOnStartDefault(boolean value) {
    editor.putBoolean("autoBackOnStartDefault", value);
    editor.apply();
  }

  public boolean getKeepAwake() {
    return sharedPreferences.getBoolean("keepAwake", true);
  }

  public void setKeepAwake(boolean value) {
    editor.putBoolean("keepAwake", value);
    editor.apply();
  }

  public boolean getDefaultShowNavBar() {
    return sharedPreferences.getBoolean("defaultShowNavBar", true);
  }

  public void setDefaultShowNavBar(boolean value) {
    editor.putBoolean("defaultShowNavBar", value);
    editor.apply();
  }

  public boolean getAutoMiniOnOutside() {
    return sharedPreferences.getBoolean("autoMiniOnOutside", false);
  }

  public void setAutoMiniOnOutside(boolean value) {
    editor.putBoolean("autoMiniOnOutside", value);
    editor.apply();
  }

  public String getDefaultDevice() {
    return sharedPreferences.getString("defaultDevice", "");
  }

  public void setDefaultDevice(String value) {
    editor.putString("defaultDevice", value);
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
