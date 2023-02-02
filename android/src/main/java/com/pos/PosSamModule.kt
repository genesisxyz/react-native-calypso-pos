package com.pos

import android.os.Build
import com.facebook.react.bridge.*
import com.pos.byteUtils.ByteConvertReactNativeUtil
import com.pos.calypso.Calypso
import com.pos.calypso.CardReadRecordsBuilder
import kotlinx.coroutines.*


class PosSamModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  private lateinit var device: CardManager

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
      device = if (isFamoco) {
        Famoco(reactApplicationContext)
      } else {
        Telpo(reactApplicationContext)
      }
      device.init(promise)
    }
  }

  @ReactMethod
  fun close() {
    device.close()
  }

  @ReactMethod
  @OptIn(DelicateCoroutinesApi::class)
  fun writeToCardUpdate(apdu: ReadableArray, options: ReadableMap, promise: Promise) {
    GlobalScope.launch {
      device.writeToCardUpdate(apdu, options, promise)
    }
  }

  @ReactMethod
  @OptIn(DelicateCoroutinesApi::class)
  fun readRecordsFromCard(options: ReadableMap, promise: Promise) {
    GlobalScope.launch {
      device.readRecordsFromCard(options, promise)
    }
  }

  companion object {
    const val NAME = "PosSam"
  }
}
