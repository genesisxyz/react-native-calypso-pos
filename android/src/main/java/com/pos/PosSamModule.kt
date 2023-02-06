package com.pos

import android.os.Build
import com.facebook.react.bridge.*
import com.pos.calypso.Calypso
import kotlinx.coroutines.*


class PosSamModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  private var device: CardManager? = null

  private val isFamoco = Build.MANUFACTURER.equals("wizarPOS")

  override fun getName(): String {
    return NAME
  }

  override fun getConstants(): MutableMap<String, Any> =
    hashMapOf(
      "ZERO_TIME_MILLIS" to Calypso.ZERO_TIME_MILLIS,
    )

  @ReactMethod
  @OptIn(DelicateCoroutinesApi::class)
  fun init(promise: Promise) {
    GlobalScope.launch {
      if (device == null) {
        if (isFamoco) {
          device = Famoco(reactApplicationContext)
        } else {
          device = Telpo(reactApplicationContext)
        }
      }
      device?.init(promise)
    }
  }

  @ReactMethod
  fun close() {
    device?.close()
  }

  @ReactMethod
  fun readCardId(promise: Promise) {
    GlobalScope.launch {
      device?.readCardId(promise)
    }
  }

  @ReactMethod
  @OptIn(DelicateCoroutinesApi::class)
  fun writeToCardUpdate(apdu: ReadableArray, options: ReadableMap, promise: Promise) {
    GlobalScope.launch {
      device?.writeToCardUpdate(apdu, options, promise)
    }
  }

  @ReactMethod
  @OptIn(DelicateCoroutinesApi::class)
  fun readRecordsFromCard(options: ReadableMap, promise: Promise) {
    GlobalScope.launch {
      device?.readRecordsFromCard(options, promise)
    }
  }

  companion object {
    const val NAME = "PosSam"
  }
}
