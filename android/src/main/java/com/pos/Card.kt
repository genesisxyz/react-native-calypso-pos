package com.pos

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableArray

abstract class Card {
  protected abstract fun open()

  // you need to call this method inside open when it's all ok
  protected abstract fun initialized(status: Boolean)

  // call open before anything else if no other promises are running
  abstract fun writeToCard(apdu: ReadableArray, promise: Promise)

  protected abstract fun getChallengeApdu(): ByteArray

  protected abstract fun transmitToSam(apdu: ByteArray): ByteArray?

  protected abstract fun transmitToCard(apdu: ByteArray): ByteArray?

  // call close inside writeToCard at the end
  protected abstract fun close(): Boolean
}
