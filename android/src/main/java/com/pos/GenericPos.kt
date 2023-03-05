package com.pos

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap

class GenericPos(private val reactContext: ReactApplicationContext): CardManager() {
  override suspend fun init(promise: Promise) {
    samId = "00 00 00"
    cardId = "123456789"
    promise.resolve(true)
  }

  override fun close() {

  }

  override fun transmitToSam(apdu: ByteArray): ByteArray? {
    return null
  }

  override fun transmitToCard(apdu: ByteArray): ByteArray? {
    return null
  }

  override fun connectSam() {
    samIsConnected = true
  }

  override fun disconnectSam() {
    samIsConnected = false
  }

  override fun connectCard() {
    cardIsConnected = true
  }

  override fun disconnectCard() {
    cardIsConnected = false
  }

  override suspend fun waitForCard() {

  }

  override suspend fun readRecordsFromCard(options: ReadableMap, promise: Promise) {
    val map = Arguments.createMap()
    val recordsMap = Arguments.createMap()
    recordsMap.putArray("1", Arguments.fromList(listOf(0x05, 0x35, 0x00, 0x04, 0x93, 0xE2, 0x00, 0xA0, 0x02, 0x8B, 0xE8, 0x22, 0x54, 0x53, 0x54, 0x54, 0x53, 0x54, 0x35, 0x36, 0x44, 0x34, 0x36, 0x4C, 0x32, 0x31, 0x39, 0x47, 0xC0)))
    map.putMap("records", recordsMap)
    map.putString("cardId", "123456789")
    promise.resolve(map)
  }
}
