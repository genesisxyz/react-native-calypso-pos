package com.pos

import com.facebook.react.bridge.*
import com.pos.byte_stuff.ByteConvertReactNativeUtil
import com.pos.byte_stuff.ByteConvertStringUtil
import java.nio.charset.StandardCharsets

class ByteUtilsModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return ByteUtilsModule.NAME
  }

  @ReactMethod
  fun stringToByteArray(str: String, promise: Promise) {
    val bytes =  ByteConvertStringUtil.stringToByteArray(str)
    promise.resolve(ByteConvertReactNativeUtil.byteArrayToReadableArray(bytes))
  }

  @ReactMethod
  fun bytesFromString(str: String, promise: Promise) {
    val bytes = str.toByteArray(StandardCharsets.UTF_8);
    promise.resolve(ByteConvertReactNativeUtil.byteArrayToReadableArray(bytes))
  }

  @ReactMethod
  fun bytesToHexString(array: ReadableArray, promise: Promise) {
    val bytes = ByteConvertReactNativeUtil.readableArrayToByteArray(array)
    val hexString = ByteConvertStringUtil.bytesToHexString(bytes)
    promise.resolve(hexString)
  }

  companion object {
    const val NAME = "ByteUtils"
  }
}
