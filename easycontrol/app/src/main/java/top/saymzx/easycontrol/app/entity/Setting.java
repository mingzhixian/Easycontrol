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

  public boolean getWakeOnConnect() {
    return sharedPreferences.getBoolean("wakeOnConnect", true);
  }

  public void setWakeOnConnect(boolean value) {
    editor.putBoolean("wakeOnConnect", value);
    editor.apply();
  }

  public boolean getLightOffOnConnect() {
    return sharedPreferences.getBoolean("lightOffOnConnect", false);
  }

  public void setLightOffOnConnect(boolean value) {
    editor.putBoolean("lightOffOnConnect", value);
    editor.apply();
  }

  public boolean getLockOnClose() {
    return sharedPreferences.getBoolean("lockOnClose", true);
  }

  public void setLockOnClose(boolean value) {
    editor.putBoolean("lockOnClose", value);
    editor.apply();
  }

  public boolean getLightOnClose() {
    return sharedPreferences.getBoolean("lightOnClose", false);
  }

  public void setLightOnClose(boolean value) {
    editor.putBoolean("lightOnClose", value);
    editor.apply();
  }

  public boolean getReconnectOnClose() {
    return sharedPreferences.getBoolean("reconnectOnClose", false);
  }

  public void setReconnectOnClose(boolean value) {
    editor.putBoolean("reconnectOnClose", value);
    editor.apply();
  }

  public boolean getAutoRotate() {
    return sharedPreferences.getBoolean("autoRotate", true);
  }

  public void setAutoRotate(boolean value) {
    editor.putBoolean("autoRotate", value);
    editor.apply();
  }

  public boolean getAutoBackOnStart() {
    return sharedPreferences.getBoolean("autoBackOnStart", false);
  }

  public void setAutoBackOnStart(boolean value) {
    editor.putBoolean("autoBackOnStart", value);
    editor.apply();
  }

  public boolean getAutoScanAddressOnStart() {
    return sharedPreferences.getBoolean("autoScanAddressStart", false);
  }

  public void setAutoScanAddressOnStart(boolean value) {
    editor.putBoolean("autoScanAddressStart", value);
    editor.apply();
  }

  public boolean getKeepAwake() {
    return sharedPreferences.getBoolean("keepAwake", true);
  }

  public void setKeepAwake(boolean value) {
    editor.putBoolean("keepAwake", value);
    editor.apply();
  }

  public boolean getShowNavBarOnConnect() {
    return sharedPreferences.getBoolean("showNavBarOnConnect", true);
  }

  public void setShowNavBarOnConnect(boolean value) {
    editor.putBoolean("showNavBarOnConnect", value);
    editor.apply();
  }

  public boolean getChangeToFullOnConnect() {
    return sharedPreferences.getBoolean("changeToFullOnConnect", false);
  }

  public void setChangeToFullOnConnect(boolean value) {
    editor.putBoolean("changeToFullOnConnect", value);
    editor.apply();
  }

  public boolean getSmallToMiniOnOutside() {
    return sharedPreferences.getBoolean("smallToMiniOnOutside", false);
  }

  public void setSmallToMiniOnOutside(boolean value) {
    editor.putBoolean("smallToMiniOnOutside", value);
    editor.apply();
  }

  public boolean getMiniRecoverOnTimeout() {
    return sharedPreferences.getBoolean("miniRecoverOnTimeout", false);
  }

  public void setMiniRecoverOnTimeout(boolean value) {
    editor.putBoolean("miniRecoverOnTimeout", value);
    editor.apply();
  }

  public boolean getFullToMiniOnExit() {
    return sharedPreferences.getBoolean("fullToMiniOnExit", true);
  }

  public void setFullToMiniOnExit(boolean value) {
    editor.putBoolean("fullToMiniOnExit", value);
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
