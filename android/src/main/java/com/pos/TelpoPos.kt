package com.pos

import com.facebook.react.bridge.*
import com.telpo.tps550.api.reader.SmartCardReader
import kotlinx.coroutines.*

class TelpoPos(private val reactContext: ReactApplicationContext) : GenericPos(reactContext) {

  private lateinit var samReader: SmartCardReader

  override suspend fun init(promise: Promise) {
    if (posIsInitialized) {
      promise.resolve(true)
      return
    }

    // try on SAM slot 1
    try {
      samReader = SmartCardReader(reactContext, SmartCardReader.SLOT_PSAM1)
      connectSam()
      posIsInitialized = true
      promise.resolve(true)
      disconnectSam()
      return;
    } catch (_: Throwable) {

    }

    // try on SAM slot 2
    try {
      samReader = SmartCardReader(reactContext, SmartCardReader.SLOT_PSAM2)
      connectSam()
      posIsInitialized = true
      promise.resolve(true)
      disconnectSam()
    } catch (e: Throwable) {
      when (e) {
        is PosException -> promise.reject(e.code, e.message)
        else -> promise.resolve(false)
      }
    }
  }

  override suspend fun writeToCardUpdate(apdu: ReadableArray, options: ReadableMap, promise: Promise) {
    job = GlobalScope.launch(start = CoroutineStart.LAZY) {
      super.writeToCardUpdate(apdu, options, promise)
    }
  }

  override fun transmitToSam(apdu: ByteArray): ByteArray? {
    return try {
      val response = samReader.transmit(apdu)
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
}
