package com.pos

import android.os.Build
import com.facebook.react.bridge.*
import com.pos.calypso.Calypso
import kotlinx.coroutines.*


class PosModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  private var device: CardManager? = null

  private val isFamoco = Build.MANUFACTURER.equals("wizarPOS")
  private val isTelpo = Build.MODEL.startsWith("TPS")
  private val isArm = Build.CPU_ABI.lowercase() in listOf("armeabi-v7a", "arm64-v8a", "armeabi")

  override fun getName(): String {
    return NAME
  }

  override fun getConstants(): MutableMap<String, Any> =
    hashMapOf(
      "ZeroTimeMillis" to Calypso.ZERO_TIME_MILLIS,
    )

  @ReactMethod
  @OptIn(DelicateCoroutinesApi::class)
  fun init(promise: Promise) {
    GlobalScope.launch {
      if (device == null) {
        if (isArm) {
          if (isFamoco) {
            device = FamocoPos(reactApplicationContext)
          } else if (isTelpo) {
            device = TelpoPos(reactApplicationContext)
          } else {
            device = GenericPos(reactApplicationContext)
          }
        } else {
          device = GenericPos(reactApplicationContext)
        }
      }
      device?.init(promise)
    }
  }

  @ReactMethod
  fun close() {
    if (isArm) {
      device?.close()
    }
  }

  @ReactMethod
  fun readCardId(promise: Promise) {
    GlobalScope.launch {
      device?.readCardId(promise)
    }
  }

  @ReactMethod
  @OptIn(DelicateCoroutinesApi::class)
  fun writeToCardUpdate(options: ReadableArray, promise: Promise) {
    GlobalScope.launch {
      device?.writeToCardUpdate(options, promise)
    }
  }

  @ReactMethod
  @OptIn(DelicateCoroutinesApi::class)
  fun readRecordsFromCard(options: ReadableArray, promise: Promise) {
    GlobalScope.launch {
      device?.readRecordsFromCard(options, promise)
    }
  }

  companion object {
    const val NAME = "Pos"
  }
}
