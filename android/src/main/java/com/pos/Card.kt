package com.pos

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableArray

abstract class Card {

  abstract suspend fun init(promise: Promise)

  abstract suspend fun readRecordsFromCard(promise: Promise)

  abstract suspend fun writeToCard(apdu: ReadableArray, promise: Promise)

  protected abstract fun transmitToSam(apdu: ByteArray): ByteArray?

  protected abstract fun transmitToCard(apdu: ByteArray): ByteArray?
}
