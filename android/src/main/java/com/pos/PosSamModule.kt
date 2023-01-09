package com.pos

import android.os.Build
import com.facebook.react.bridge.*

class PosSamModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  private val isFamoco = Build.MANUFACTURER.equals("wizarPOS")

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun init(promise: Promise) {
    if (isFamoco) {
      val famoco = Famoco(reactApplicationContext)
      famoco.init(promise)
    } else {
      val telpo = Telpo(reactApplicationContext)
      telpo.init(promise)
    }
  }

  @ReactMethod
  fun writeToCard(apdu: ReadableArray, promise: Promise) {
    if (isFamoco) {
      val famoco = Famoco(reactApplicationContext)
      famoco.writeToCard(apdu, promise)
    } else {
      val telpo = Telpo(reactApplicationContext)
      telpo.writeToCard(apdu, promise)
    }
  }

  companion object {
    const val NAME = "PosSam"
  }
}
