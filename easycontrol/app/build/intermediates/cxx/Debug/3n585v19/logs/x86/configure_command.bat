@echo off
"C:\\Users\\kic\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HE:\\Code\\easycontrol\\easycontrol\\app\\src\\main\\cpp" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=23" ^
  "-DANDROID_PLATFORM=android-23" ^
  "-DANDROID_ABI=x86" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86" ^
  "-DANDROID_NDK=C:\\Users\\kic\\AppData\\Local\\Android\\Sdk\\ndk\\25.1.8937393" ^
  "-DCMAKE_ANDROID_NDK=C:\\Users\\kic\\AppData\\Local\\Android\\Sdk\\ndk\\25.1.8937393" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\Users\\kic\\AppData\\Local\\Android\\Sdk\\ndk\\25.1.8937393\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\Users\\kic\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=E:\\Code\\easycontrol\\easycontrol\\app\\build\\intermediates\\cxx\\Debug\\3n585v19\\obj\\x86" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=E:\\Code\\easycontrol\\easycontrol\\app\\build\\intermediates\\cxx\\Debug\\3n585v19\\obj\\x86" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BE:\\Code\\easycontrol\\easycontrol\\app\\.cxx\\Debug\\3n585v19\\x86" ^
  -GNinja
