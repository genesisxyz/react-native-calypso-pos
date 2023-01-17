package com.pos.byteUtils

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray

class ByteConvertReactNativeUtil {
  companion object {
    fun byteArrayToReadableArray(bytes: ByteArray): ReadableArray {
      val array = Arguments.createArray()
      bytes.forEach {
        array.pushInt(it.toInt())
      }
      return array;
    }

    fun readableArrayToByteArray(array: ReadableArray): ByteArray {
      val bytes = (array.toArrayList() as ArrayList<Int>).map { it.toByte() }.toByteArray()
      return bytes
    }
  }
}
