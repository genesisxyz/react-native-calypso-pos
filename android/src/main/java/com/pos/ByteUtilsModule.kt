package com.pos

import com.facebook.react.bridge.*
import com.pos.byte_stuff.ByteConvertReactNativeUtil
import com.pos.byte_stuff.ByteConvertStringUtil
import java.math.BigInteger
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
  fun makeByteArrayFromString(str: String, promise: Promise) {
    val byteCount = str.split(Regex("\\s+")).size
    val bytes = ByteArray(byteCount)
    ByteConvertStringUtil.stringToByteArray(str, bytes)
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

  @ReactMethod
  fun byteToHexString(byte: Double, promise: Promise) {
    val hexString = ByteConvertStringUtil.byteToHexString(byte.toInt().toByte())
    promise.resolve(hexString)
  }

  @ReactMethod
  fun shiftRight(array: ReadableArray, n: Int, promise: Promise) {
    val bytes = ByteConvertReactNativeUtil.readableArrayToByteArray(array)
    val bigInteger = BigInteger(bytes).shiftRight(n)
    promise.resolve(ByteConvertReactNativeUtil.byteArrayToReadableArray(bigInteger.toByteArray()))
  }

  @ReactMethod
  fun stringFromByteArray(array: ReadableArray, promise: Promise) {
    val bytes = ByteConvertReactNativeUtil.readableArrayToByteArray(array)
    promise.resolve(String(bytes))
  }

  companion object {
    const val NAME = "ByteUtils"
  }
}
