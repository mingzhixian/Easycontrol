package top.saymzx.easycontrol.app.entity;

public final class MyInterface {
  public interface MyFunction {
    void run();
  }

  public interface MyFunctionBoolean {
    void run(Boolean bool);
  }

  public interface MyFunctionString {
    void run(String str);
  }

  public interface MyFunctionInt {
    void run(int value);
  }

  public interface MyFunctionBytes {
    void run(byte[] buffer);
  }
}
