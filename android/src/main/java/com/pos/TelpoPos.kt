package com.pos

import com.facebook.react.bridge.*
import com.telpo.tps550.api.reader.SmartCardReader
import kotlinx.coroutines.*

class TelpoPos(private val reactContext: ReactApplicationContext) : GenericPos(reactContext) {

  private lateinit var samReader: SmartCardReader
  private var samReaderIsInitialized = false

  private fun initializeSamReader() {
    if (samReaderIsInitialized) {
      return
    }

    // try on SAM slot 1
    try {
      samReader = SmartCardReader(reactContext, SmartCardReader.SLOT_PSAM1)
      connectSam()
      samReaderIsInitialized = true
      disconnectSam()
      return
    } catch (_: Throwable) {

    }

    // try on SAM slot 2
    try {
      samReader = SmartCardReader(reactContext, SmartCardReader.SLOT_PSAM2)
      connectSam()
      samReaderIsInitialized = true
      disconnectSam()
      return
    } catch (e: Throwable) {

    }

    // try on SAM slot 3
    try {
      samReader = SmartCardReader(reactContext, SmartCardReader.SLOT_PSAM3)
      connectSam()
      samReaderIsInitialized = true
      disconnectSam()
      return
    } catch (e: Throwable) {

    }

    // try on SAM slot 4
    try {
      samReader = SmartCardReader(reactContext, SmartCardReader.SLOT_PSAM4)
      connectSam()
      samReaderIsInitialized = true
      disconnectSam()
    } catch (e: Throwable) {
      throw PosException(PosException.NO_SAM_AVAILABLE, "No SAM available")
    }
  }

  override fun transmitToSam(apdu: ByteArray): ByteArray? {
    return try {
      val response = samReader.transmit(apdu)
      if (response.size == 2 && response[0] == 0x61.toByte()) {
        val sizeResponseToRequest = response[1]

        val getResponseCommandBytes = byteArrayOf(0x80.toByte(), 0xC0.toByte(), 0x00, 0x00,
          sizeResponseToRequest)

        val newResult = samReader.transmit(getResponseCommandBytes)
        return newResult
      }
      response
    } catch (e: NullPointerException) {
      null
    }
  }

  override fun connectSam() {
    if (samIsConnected) return
    val isOpen = samReader.open()
    val isIccPowerOn = samReader.iccPowerOn()

    if (!isOpen || !isIccPowerOn) {
      throw PosException(PosException.SAM_CONNECT_FAIL, "Cannot connect to SAM")
    }

    val tempSamId = samReader.atrString
    if (tempSamId != null) samId = tempSamId.substring(24, 32)

    samIsConnected = true
  }

  override fun disconnectSam() {
    if (!samIsConnected) return
    samReader.iccPowerOff()
    samReader.close()
    samIsConnected = false
  }

  override fun write(options: ReadableArray, promise: Promise) {
    initializeSamReader()
    super.write(options, promise)
  }

  override fun getSamId(promise: Promise) {
    try {
      initializeSamReader()
      super.getSamId(promise)
    } catch (e: Exception) {
      val userInfo = Arguments.createMap()
      userInfo.putBoolean("isPosError", true)
      when (e) {
        is PosException -> promise.reject(e.code, e.message, userInfo)
        else -> promise.reject(PosException.UNKNOWN, "Unknown", userInfo)
      }
    } finally {
      disconnectSam()
    }
  }
}
