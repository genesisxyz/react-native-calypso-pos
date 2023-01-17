package com.pos

import com.cloudpos.*
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
import com.pos.byteUtils.ByteConvertReactNativeUtil
import com.pos.byteUtils.ByteConvertStringUtil
import com.pos.calypso.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Famoco(private val reactContext: ReactApplicationContext): com.pos.Card() {
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

  override suspend fun init(promise: Promise) {
    try {
      famocoRFCardReaderDevice = POSTerminal.getInstance(reactContext).getDevice("cloudpos.device.rfcardreader") as RFCardReaderDevice
      famocoSmartCardReaderDevice = POSTerminal.getInstance(reactContext).getDevice("cloudpos.device.smartcardreader", famocoSamLogicalID) as SmartCardReaderDevice

      openSamReader()
      getSamCard()
      promise.resolve(true)
      closeSamReader()
    } catch (e: Throwable) {
      e.printStackTrace()
      promise.resolve(false)
    }
  }

  override suspend fun waitForCard() = suspendCancellableCoroutine<Unit> { cont ->
    try {
      var cardId: String? = null

      val operationResult: OperationResult = famocoRFCardReaderDevice.waitForCardPresent(TimeConstants.FOREVER)
      if (operationResult.resultCode == OperationResult.SUCCESS) {
        rfCard = (operationResult as RFCardReaderOperationResult).card
        // TODO: why do you need the next 2 lines?
        rfCard.id
        rfCard.cardStatus
        val cardIdHex = ByteConvertStringUtil.bytesToHexString(rfCard.id)
        if (cardIdHex != null) {
          cardId =
            cardIdHex.replace("\\s+".toRegex(), "").toLong(16).toString()
        }
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
      waitForCard()
      connectCard()

      val selectApplicationBuilder = SelectApplicationBuilder(
        SelectApplicationBuilder.SELECT_FIRST_OCCURRENCE_RETURN_FCI)

      val selectApplicationResponseAdapter = ApduResponseAdapter(transmitToCard(selectApplicationBuilder.apdu))

      val selectApplicationParser = selectApplicationBuilder.createResponseParser(selectApplicationResponseAdapter)

      selectApplicationParser.checkStatus()

      // TODO is this necessary?
      /*
      if (!selectApplicationParser.isBipApplication) {
        throw PosException(PosException.CARD_NOT_SUPPORTED, "Card not supported")
      }
      */

      val selectFileBuilder = CardSelectFileBuilder(Calypso.LID_EF_ENVIRONMENT)

      val selectFileResponseAdapter = ApduResponseAdapter(transmitToCard(selectFileBuilder.apdu))

      val selectFileParser = selectFileBuilder
        .createResponseParser(selectFileResponseAdapter)

      selectFileParser.checkStatus()

      if (!selectFileParser.isSuccess || selectFileParser.proprietaryInformation == null) {
        throw PosException(PosException.CARD_NOT_SUPPORTED, "Card not supported")
      }

      val readRecordsBuilder = CardReadRecordsBuilder(
        Calypso.SFI_EF_ENVIRONMENT, 1,
        CardReadRecordsBuilder.ReadMode.ONE_RECORD, 0)

      val readRecordsResponseAdapter = ApduResponseAdapter(transmitToCard(readRecordsBuilder.apdu))

      val readRecordsParser = readRecordsBuilder.createResponseParser(readRecordsResponseAdapter)

      readRecordsParser.checkStatus()

      val records: Map<Int, ByteArray> = readRecordsParser.records

      val readableMap = Arguments.createMap();
      for ((key, record) in records) {
        val array = ByteConvertReactNativeUtil.byteArrayToReadableArray(record)
        readableMap.putArray(key.toString(), array)
      }

      promise.resolve(readableMap)
    } catch (e: Throwable) {
      e.printStackTrace()
      promise.reject(
        when (e) {
          is PosException -> e
          is CardCommandException -> PosException(PosException.TRANSMIT_APDU_COMMAND, "Command status failed")
          else -> PosException(PosException.CARD_NOT_PRESENT, "Card not present")
        }
      )
    } finally {
      disconnectCard()
      closeCardReader()
    }
  }

  override suspend fun writeToCard(apdu: ReadableArray, promise: Promise) {
    openCardReader()
    super.writeToCard(apdu, promise)
    closeCardReader()
  }

  override fun connectSam() {
    openSamReader()
    try {
      (samCard as CPUCard).connect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      closeSamReader()
      throw e
    }
  }

  override fun disconnectSam() {
    try {
      (samCard as CPUCard).disconnect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw e
      }
    } finally {
        closeSamReader()
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
    try {
      (rfCard as CPUCard).connect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      throw e
    }
  }

  override fun disconnectCard() {
    try {
      (rfCard as CPUCard).disconnect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw e
      }
    }
  }

  override fun transmitToCard(apdu: ByteArray): ByteArray? {
    return try {
      (rfCard as CPUCard).transmit(apdu)
    } catch (e: Exception) {
      throw PosException(PosException.TRANSMIT_APDU_COMMAND, "Transmit apdu command error")
    }
  }

  private fun openSamReader() {
    try {
      famocoSmartCardReaderDevice.open(famocoSamLogicalID)
    } catch (e: DeviceException) {
      if (e.code == -1) {
        throw e
      }
    }
  }

  private fun closeSamReader() {
    try {
      famocoSmartCardReaderDevice.close()
    } catch (e: DeviceException) {
      if (e.code == -1) {
        throw e
      }
    }
  }

  private fun openCardReader() {
    try {
      famocoRFCardReaderDevice.open()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw e
      }
    }
  }

  private fun closeCardReader() {
    try {
      famocoRFCardReaderDevice.close()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw e
      }
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
}
