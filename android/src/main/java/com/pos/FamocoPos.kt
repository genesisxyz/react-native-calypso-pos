package com.pos

import com.cloudpos.DeviceException
import com.cloudpos.OperationListener
import com.cloudpos.OperationResult
import com.cloudpos.POSTerminal
import com.cloudpos.TimeConstants
import com.cloudpos.card.CPUCard
import com.cloudpos.card.Card
import com.cloudpos.rfcardreader.RFCardReaderDevice
import com.cloudpos.rfcardreader.RFCardReaderOperationResult
import com.cloudpos.smartcardreader.SmartCardReaderDevice
import com.cloudpos.smartcardreader.SmartCardReaderOperationResult
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.pos.byteUtils.ByteConvertStringUtil
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class FamocoPos(private val reactContext: ReactApplicationContext) : CardManager() {
  private var samCard: Card? = null
  private lateinit var rfCard: Card

  private val famocoSamLogicalID = 1
  private lateinit var famocoRFCardReaderDevice: RFCardReaderDevice
  private lateinit var famocoSmartCardReaderDevice: SmartCardReaderDevice

  private suspend fun getSamCard() = suspendCancellableCoroutine<Unit> { cont ->
    val listener = OperationListener { arg0: OperationResult ->
      try {
        if (arg0.resultCode == OperationResult.SUCCESS) {
          samCard = (arg0 as SmartCardReaderOperationResult).card
          val tempSamId = ByteConvertStringUtil.bytesToHexString(samCard!!.id)
          if (tempSamId != null) samId = tempSamId.substring(36, 47).replace("\\s+".toRegex(), "")
          cont.resume(Unit)
        } else {
          cont.resumeWithException(PosException(PosException.NO_SAM_AVAILABLE, "No SAM available"))
        }
      } catch (e: Throwable) {
        cont.resumeWithException(e)
      }
    }
    famocoSmartCardReaderDevice.listenForCardPresent(listener, TimeConstants.FOREVER)
  }

  private var posIsInitialized = false

  override suspend fun init(promise: Promise) {
    if (posIsInitialized) {
      promise.resolve(true)
      return
    }

    try {
      famocoRFCardReaderDevice = POSTerminal.getInstance(reactContext).getDevice(POSTerminal.DEVICE_NAME_RF_CARD_READER) as RFCardReaderDevice
      famocoSmartCardReaderDevice = POSTerminal.getInstance(reactContext).getDevice(POSTerminal.DEVICE_NAME_SMARTCARD_READER, famocoSamLogicalID) as SmartCardReaderDevice

      // close if for some reason the sam reader is already open
      try {
        famocoRFCardReaderDevice.close()
      } catch (e: DeviceException) {
        e.printStackTrace()
      }
      openSamReader()
      getSamCard()
      posIsInitialized = true
      promise.resolve(true)
      closeSamReader()
    } catch (e: Throwable) {
      e.printStackTrace()
      promise.resolve(true)
    }
  }

  override suspend fun waitForCard() = suspendCancellableCoroutine<Unit> { cont ->
    try {
      val operationResult: OperationResult = famocoRFCardReaderDevice.waitForCardPresent(TimeConstants.FOREVER)
      if (operationResult.resultCode == OperationResult.SUCCESS) {
        rfCard = (operationResult as RFCardReaderOperationResult).card
        // TODO: why do you need the next 2 lines?
        rfCard.id
        rfCard.cardStatus
        val cardIdHex = ByteConvertStringUtil.bytesToHexString(rfCard.id)
        cardId = cardIdHex.replace("\\s+".toRegex(), "").toLong(16).toString()

        val params = Arguments.createMap().apply {
          putString("status", "detected")
        }
        sendEvent(reactContext, "CardStatus", params)

        cont.resume(Unit)
      } else if (operationResult.resultCode == OperationResult.CANCEL) {
        throw PosException(PosException.CANCEL, "Cancel")
      } else {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
      }
    } catch (e: DeviceException) {
      e.printStackTrace()
      // pending request
      if (e.code == -3) {
        cancelCardRequest()
        cont.resumeWithException(PosException(PosException.PENDING_REQUEST, "Pending request"))
      } else {
        cont.resumeWithException(e)
      }
    }
  }

  override fun connectSam() {
    if (samCard === null) {
      throw PosException(PosException.NO_SAM_AVAILABLE, "No SAM available")
    }
    openSamReader()
    if (samIsConnected) return
    samIsConnected = try {
      (samCard as CPUCard).connect()
      true
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw PosException(PosException.SAM_CONNECT_FAIL, "Sam connect fail")
      }
      true
    }
  }

  override fun disconnectSam() {
    if (!samIsConnected) return
    samIsConnected = try {
      (samCard as CPUCard?)?.disconnect()
      closeSamReader()
      false
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw PosException(PosException.SAM_DISCONNECT_FAIL, "Sam disconnect fail")
      }
      false
    }
  }

  override fun transmitToSam(apdu: ByteArray): ByteArray? {
    return try {
      (samCard as CPUCard).transmit(apdu)
    } catch (e: Exception) {
      throw PosException(PosException.TRANSMIT_APDU_COMMAND, "Transmit apdu command error")
    }
  }

  override fun connectCard() {
    if (cardIsConnected) return
    cardIsConnected = try {
      (rfCard as CPUCard).connect()
      true
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
      }
      true
    }
  }

  override fun disconnectCard() {
    cardIsConnected = false
    // TODO: is this safe? should we remove this block below?
    /*
    if (!cardIsConnected) return
    // we need a thread because otherwise it blocks the main thread until we move the card away from the reader
    // TODO: why does GlobalScope work and a normal thread doesn't?
    GlobalScope.launch {
      cardIsConnected = try {
        (rfCard as CPUCard).disconnect()
        false
      } catch (e: DeviceException) {
        e.printStackTrace()
        if (e.code == -1) {
          // TODO: do we actually care about this?
          throw PosException(PosException.CARD_DISCONNECT_FAIL, "Card disconnect fail")
        }
        false
      }
    }
    */
  }

  override fun transmitToCard(apdu: ByteArray): ByteArray? {
    return try {
      (rfCard as CPUCard).transmit(apdu)
    } catch (e: Exception) {
      e.printStackTrace()
      throw PosException(PosException.TRANSMIT_APDU_COMMAND, "Transmit apdu command error")
    }
  }

  private fun openSamReader() {
    if (samReaderIsConnected) return
    samReaderIsConnected = try {
      famocoSmartCardReaderDevice.open(famocoSamLogicalID)
      true
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw PosException(PosException.SAM_CONNECT_FAIL, "Sam connect fail")
      }
      true
    }
  }

  private fun closeSamReader() {
    if (!samReaderIsConnected) return
    samReaderIsConnected = try {
      famocoSmartCardReaderDevice.close()
      false
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw PosException(PosException.SAM_DISCONNECT_FAIL, "Sam disconnect fail")
      }
      false
    }
  }

  private fun openCardReader() {
    if (cardReaderIsConnected) return
    cardReaderIsConnected = try {
      famocoRFCardReaderDevice.open()
      true
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw PosException(PosException.CARD_CONNECT_FAIL, "Card connect fail")
      }
      true
    }
  }

  private fun closeCardReader() {
    if (!cardReaderIsConnected) return
    cardReaderIsConnected = try {
      famocoRFCardReaderDevice.close()
      false
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw PosException(PosException.CARD_DISCONNECT_FAIL, "Card disconnect fail")
      }
      false
    }
  }

  private fun cancelCardRequest() {
    try {
      famocoRFCardReaderDevice.cancelRequest()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw PosException(PosException.CARD_DISCONNECT_FAIL, "Card disconnect fail")
      }
    }
  }

  private var samReaderIsConnected = false
  private var cardReaderIsConnected = false

  override suspend fun close() {
    try {
      closeCardReader()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    try {
      closeSamReader()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override suspend fun unsafeWaitForCard(promise: Promise) {
    try {
      openCardReader()
      waitForCard()

      val response = Arguments.createMap()
      response.putString("cardId", cardId)
      promise.resolve(response)
    } catch (e: Exception) {
      val userInfo = Arguments.createMap()
      userInfo.putBoolean("isPosError", true)
      when (e) {
        is PosException -> promise.reject(e.code, e.message, userInfo)
        else -> promise.reject(PosException.UNKNOWN, "Unknown", userInfo)
      }
    }
  }

  override fun unsafeDisconnectCard(promise: Promise) {
    try {
      disconnectCard()
      closeCardReader()
      promise.resolve(true)
    } catch (e: Exception) {
      val userInfo = Arguments.createMap()
      userInfo.putBoolean("isPosError", true)
      when (e) {
        is PosException -> promise.reject(e.code, e.message, userInfo)
        else -> promise.reject(PosException.UNKNOWN, "Unknown", userInfo)
      }
    }
  }
}
