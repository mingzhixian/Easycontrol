package top.saymzx.scrcpy.server.wrappers

import android.content.ClipData
import android.content.IOnPrimaryClipChangedListener
import android.os.Build
import android.os.IInterface
import top.saymzx.scrcpy.server.helper.FakeContext
import java.lang.reflect.Method

class ClipboardManager(private val manager: IInterface) {

  private var getPrimaryClipMethod: Method
  private var setPrimaryClipMethod: Method
  private var addPrimaryClipChangedListener: Method

  private var getMethodVersion = 0
  private var setMethodVersion = 0
  private var addListenerMethodVersion = 0

  val text: CharSequence?
    get() = try {
      val clipData = getPrimaryClip(getPrimaryClipMethod, getMethodVersion, manager)
      if (clipData.itemCount == 0) {
        null
      } else clipData.getItemAt(0).text
    } catch (_: Exception) {
      null
    }

  init {
    getPrimaryClipMethod = forGetPrimaryClipMethod()
    setPrimaryClipMethod = forSetPrimaryClipMethod()
    addPrimaryClipChangedListener = forAddPrimaryClipChangedListener()
  }

  private fun forGetPrimaryClipMethod(): Method {
    var field: Method
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      field = manager.javaClass.getMethod("getPrimaryClip", String::class.java)
    } else {
      try {
        field = manager.javaClass.getMethod(
          "getPrimaryClip",
          String::class.java,
          Int::class.javaPrimitiveType
        )
        getMethodVersion = 0
      } catch (e1: NoSuchMethodException) {
        try {
          field = manager.javaClass.getMethod(
            "getPrimaryClip",
            String::class.java,
            String::class.java,
            Int::class.javaPrimitiveType
          )
          getMethodVersion = 1
        } catch (e2: NoSuchMethodException) {
          try {
            field = manager.javaClass.getMethod(
              "getPrimaryClip",
              String::class.java,
              String::class.java,
              Int::class.javaPrimitiveType,
              Int::class.javaPrimitiveType
            )
            getMethodVersion = 2
          } catch (e3: NoSuchMethodException) {
            field = manager.javaClass.getMethod(
              "getPrimaryClip",
              String::class.java,
              Int::class.javaPrimitiveType,
              String::class.java
            )
            getMethodVersion = 3
          }
        }
      }
    }
    return field
  }

  private fun forSetPrimaryClipMethod(): Method {
    var field: Method
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      field =
        manager.javaClass.getMethod("setPrimaryClip", ClipData::class.java, String::class.java)
    } else {
      try {
        field = manager.javaClass.getMethod(
          "setPrimaryClip",
          ClipData::class.java,
          String::class.java,
          Int::class.javaPrimitiveType
        )
        setMethodVersion = 0
      } catch (e1: NoSuchMethodException) {
        try {
          field = manager.javaClass.getMethod(
            "setPrimaryClip",
            ClipData::class.java,
            String::class.java,
            String::class.java,
            Int::class.javaPrimitiveType
          )
          setMethodVersion = 1
        } catch (e2: NoSuchMethodException) {
          field = manager.javaClass
            .getMethod(
              "setPrimaryClip",
              ClipData::class.java,
              String::class.java,
              String::class.java,
              Int::class.javaPrimitiveType,
              Int::class.javaPrimitiveType
            )
          setMethodVersion = 2
        }
      }
    }
    return field
  }

  private fun forAddPrimaryClipChangedListener(): Method {
    var field: Method
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      field = manager.javaClass
        .getMethod(
          "addPrimaryClipChangedListener",
          IOnPrimaryClipChangedListener::class.java,
          String::class.java
        )
    } else {
      try {
        field = manager.javaClass
          .getMethod(
            "addPrimaryClipChangedListener",
            IOnPrimaryClipChangedListener::class.java,
            String::class.java,
            Int::class.javaPrimitiveType
          )
        addListenerMethodVersion = 0
      } catch (e1: NoSuchMethodException) {
        try {
          field = manager.javaClass
            .getMethod(
              "addPrimaryClipChangedListener",
              IOnPrimaryClipChangedListener::class.java,
              String::class.java,
              String::class.java,
              Int::class.javaPrimitiveType
            )
          addListenerMethodVersion = 1
        } catch (e2: NoSuchMethodException) {
          field = manager.javaClass
            .getMethod(
              "addPrimaryClipChangedListener",
              IOnPrimaryClipChangedListener::class.java,
              String::class.java,
              String::class.java,
              Int::class.javaPrimitiveType,
              Int::class.javaPrimitiveType
            )
          addListenerMethodVersion = 2
        }
      }
    }
    return field
  }

  fun setText(text: CharSequence?): Boolean {
    return try {
      val method = setPrimaryClipMethod
      val clipData = ClipData.newPlainText(null, text)
      setPrimaryClip(method, setMethodVersion, manager, clipData)
      true
    } catch (_: Exception) {
      false
    }
  }

  fun addPrimaryClipChangedListener(listener: IOnPrimaryClipChangedListener): Boolean {
    return try {
      val method = addPrimaryClipChangedListener
      addPrimaryClipChangedListener(method, addListenerMethodVersion, manager, listener)
      true
    } catch (_: Exception) {
      false
    }
  }

  companion object {
    private fun getPrimaryClip(method: Method?, methodVersion: Int, manager: IInterface): ClipData {
      return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        method!!.invoke(manager, FakeContext.PACKAGE_NAME) as ClipData
      } else when (methodVersion) {
        0 -> method!!.invoke(manager, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID) as ClipData
        1 -> method!!.invoke(
          manager,
          FakeContext.PACKAGE_NAME,
          null,
          FakeContext.ROOT_UID
        ) as ClipData

        2 -> method!!.invoke(
          manager,
          FakeContext.PACKAGE_NAME,
          null,
          FakeContext.ROOT_UID,
          0
        ) as ClipData

        else -> method!!.invoke(
          manager,
          FakeContext.PACKAGE_NAME,
          FakeContext.ROOT_UID,
          null
        ) as ClipData
      }
    }

    private fun setPrimaryClip(
      method: Method?,
      methodVersion: Int,
      manager: IInterface,
      clipData: ClipData
    ) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        method!!.invoke(manager, clipData, FakeContext.PACKAGE_NAME)
        return
      }
      when (methodVersion) {
        0 -> method!!.invoke(manager, clipData, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID)
        1 -> method!!.invoke(
          manager,
          clipData,
          FakeContext.PACKAGE_NAME,
          null,
          FakeContext.ROOT_UID
        )

        else -> method!!.invoke(
          manager,
          clipData,
          FakeContext.PACKAGE_NAME,
          null,
          FakeContext.ROOT_UID,
          0
        )
      }
    }

    private fun addPrimaryClipChangedListener(
      method: Method?,
      methodVersion: Int,
      manager: IInterface,
      listener: IOnPrimaryClipChangedListener
    ) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        method!!.invoke(manager, listener, FakeContext.PACKAGE_NAME)
        return
      }
      when (methodVersion) {
        0 -> method!!.invoke(manager, listener, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID)
        1 -> method!!.invoke(
          manager,
          listener,
          FakeContext.PACKAGE_NAME,
          null,
          FakeContext.ROOT_UID
        )

        else -> method!!.invoke(
          manager,
          listener,
          FakeContext.PACKAGE_NAME,
          null,
          FakeContext.ROOT_UID,
          0
        )
      }
    }
  }
}