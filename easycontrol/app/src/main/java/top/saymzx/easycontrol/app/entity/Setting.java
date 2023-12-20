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

  public boolean getDefaultUseTunnel() {
    return sharedPreferences.getBoolean("defaultUseTunnel", false);
  }

  public void setDefaultUseTunnel(boolean value) {
    editor.putBoolean("defaultUseTunnel", value);
    editor.apply();
  }

  public boolean getDefaultTurnOffScreen() {
    return sharedPreferences.getBoolean("defaultTurnOffScreen", true);
  }

  public void setDefaultTurnOffScreen(boolean value) {
    editor.putBoolean("defaultTurnOffScreen", value);
    editor.apply();
  }

  public boolean getDefaultAutoLockAfterControl() {
    return sharedPreferences.getBoolean("defaultAutoLockAfterControl", true);
  }

  public void setDefaultAutoLockAfterControl(boolean value) {
    editor.putBoolean("defaultAutoLockAfterControl", value);
    editor.apply();
  }

  public boolean getDefaultFull() {
    return sharedPreferences.getBoolean("defaultFull", false);
  }

  public void setDefaultFull(boolean value) {
    editor.putBoolean("defaultFull", value);
    editor.apply();
  }

  public boolean getDefaultShowNavBar() {
    return sharedPreferences.getBoolean("defaultShowNavBar", true);
  }

  public void setDefaultShowNavBar(boolean value) {
    editor.putBoolean("defaultShowNavBar", value);
    editor.apply();
  }

  public boolean getMasterAudoRotation() {
    return sharedPreferences.getBoolean("masterAudoRotation", true);
  }

  public void setMasterAudoRotation(boolean value) {
    editor.putBoolean("masterAudoRotation", value);
    editor.apply();
  }

  public boolean getSlaveAudoRotation() {
    return sharedPreferences.getBoolean("slaveAudoRotation", true);
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
