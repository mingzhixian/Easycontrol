package top.saymzx.easycontrol.app.client;

public interface HandleUi {
  int StartControl = 1;
  int ChangeRotation = 2;
  int StopControl = 3;
  int UpdateFps=4;
  int UpdateDelay=5;

  void handle(int mode, int arg);
}
