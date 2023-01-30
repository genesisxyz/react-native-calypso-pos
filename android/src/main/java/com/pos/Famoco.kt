package com.pos

import com.cloudpos.*
import com.cloudpos.card.CPUCard
import com.cloudpos.card.Card
import com.cloudpos.rfcardreader.RFCardReaderDevice
import com.cloudpos.rfcardreader.RFCardReaderOperationResult
import com.cloudpos.smartcardreader.SmartCardReaderDevice
import com.cloudpos.smartcardreader.SmartCardReaderOperationResult
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.pos.byteUtils.ByteConvertStringUtil
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Famoco(private val reactContext: ReactApplicationContext) : CardManager() {
  private lateinit var samId: String
  private lateinit var samCard: Card
  private lateinit var rfCard: Card

  private val famocoSamLogicalID = 1
  private lateinit var famocoRFCardReaderDevice: RFCardReaderDevice
  private lateinit var famocoSmartCardReaderDevice: SmartCardReaderDevice

  private suspend fun getSamCard() = suspendCancellableCoroutine<Unit> { cont ->
    val listener = OperationListener { arg0: OperationResult ->
      try {
        if (arg0.resultCode == OperationResult.SUCCESS) {
          samCard = (arg0 as SmartCardReaderOperationResult).card
          val tempSamId = ByteConvertStringUtil.bytesToHexString(samCard.id)
          if (tempSamId != null) samId = tempSamId.substring(24, 32)
          cont.resume(Unit)
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
      promise.resolve(false)
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
        cont.resume(Unit)
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

  override suspend fun readRecordsFromCard(promise: Promise) {
    try {
      openCardReader()
      super.readRecordsFromCard(promise)
      closeCardReader()
    } catch (e: Throwable) {
      promise.reject(e)
    }
  }

  override suspend fun writeToCard(apdu: ReadableArray, promise: Promise) {
    try {
      openSamReader()
      openCardReader()
      super.writeToCard(apdu, promise)
      closeCardReader()
      closeSamReader()
    } catch (e: Throwable) {
      promise.reject(e)
    }
  }

  override fun connectSam() {
    if (samIsConnected) return
    samIsConnected = try {
      (samCard as CPUCard).connect()
      true
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw e
      }
      true
    }
  }

  override fun disconnectSam() {
    if (!samIsConnected) return
    samIsConnected = try {
      (samCard as CPUCard).disconnect()
      false
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw e
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
        throw e
      }
      true
    }
  }

  override fun disconnectCard() {
    if (!cardIsConnected) return
    cardIsConnected = try {
      (rfCard as CPUCard).disconnect()
      false
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw e
      }
      false
    }
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
        throw e
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
        throw e
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
        throw e
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
        throw e
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
        throw e
      }
    }
  }

  private var samReaderIsConnected = false
  private var cardReaderIsConnected = false

  override fun close() {
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
}
