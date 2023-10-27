package top.saymzx.easycontrol.app.entity;

import android.content.SharedPreferences;

public final class Setting {
  private final SharedPreferences sharedPreferences;

  private final SharedPreferences.Editor editor;

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

  public boolean getSlaveTurnOffScreen() {
    return sharedPreferences.getBoolean("slaveTurnOffScreen", true);
  }

  public void setSlaveTurnOffScreen(boolean value) {
    editor.putBoolean("slaveTurnOffScreen", value);
    editor.apply();
  }

  public boolean getDefaultFull() {
    return sharedPreferences.getBoolean("defaultFull", false);
  }

  public void setDefaultFull(boolean value) {
    editor.putBoolean("defaultFull", value);
    editor.apply();
  }

  public int getDefaultDevice() {
    return sharedPreferences.getInt("defaultDevice", -1);
  }

  public void setDefaultDevice(int value) {
    editor.putInt("defaultDevice", value);
    editor.apply();
  }

  public Setting(SharedPreferences sharedPreferences) {
    this.sharedPreferences = sharedPreferences;
    this.editor = sharedPreferences.edit();
  }
}
