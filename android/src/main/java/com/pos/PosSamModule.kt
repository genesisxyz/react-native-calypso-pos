package com.pos

import android.os.Build
import android.widget.Toast
import com.facebook.react.bridge.*
import com.pos.byte_stuff.ByteConvertReactNativeUtil
import com.pos.byte_stuff.ByteUtils
import com.pos.calypso.Calypso


class PosSamModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  private lateinit var device: Card

  private val isFamoco = Build.MANUFACTURER.equals("wizarPOS")

  override fun getName(): String {
    return NAME
  }

  override fun getConstants(): MutableMap<String, Any> {
    val constants: MutableMap<String, Any> = HashMap()
    constants["ZERO_TIME_MILLIS"] = Calypso.ZERO_TIME_MILLIS
    constants["SAM_CHALLENGE_LENGTH_BYTES"] = Calypso.SAM_CHALLENGE_LENGTH_BYTES
    constants["AID"] = ByteConvertReactNativeUtil.byteArrayToReadableArray(Calypso.AID)
    return constants
  }

  @ReactMethod
  fun init(promise: Promise) {
    device = if (isFamoco) {
      Famoco(reactApplicationContext)
    } else {
      Telpo(reactApplicationContext)
    }
    device.init(promise)
  }

  @ReactMethod
  fun writeToCard(apdu: ReadableArray, promise: Promise) {
    device.writeToCard(apdu, promise)
  }

  @ReactMethod
  fun readRecordsFromCard(promise: Promise) {
    device.readRecordsFromCard(promise)
  }

  companion object {
    const val NAME = "PosSam"
  }
}
