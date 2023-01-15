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
import com.pos.byte_stuff.ByteConvertReactNativeUtil
import com.pos.byte_stuff.ByteConvertStringUtil
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

  private val CP_SAM_UNLOCK_STRING: String = "62 EE D0 33 FB 9F D1 85 B3 C7 DA BD 02 82 D6 EC"

  private suspend fun waitForCard() = suspendCancellableCoroutine<Unit> { cont ->
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
      try {
        openCardReader()
        waitForCard()
        connectSam()
        connectCard()

        val newRecord = (apdu.toArrayList() as ArrayList<Int>).map { it.toByte() }.toByteArray();

        val commandsSequenceBuilder = StringBuilder()

        // preparing select application command
        val selectApplicationBuilder = SelectApplicationBuilder(
          SelectApplicationBuilder.SELECT_FIRST_OCCURRENCE_RETURN_FCI)

        commandsSequenceBuilder.append(
          ByteConvertStringUtil.bytesToHexString(
            selectApplicationBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter
        val selectApplicationResponseAdapter = ApduResponseAdapter(
          transmitToCard(selectApplicationBuilder.apdu))

        commandsSequenceBuilder.append(
          ByteConvertStringUtil.bytesToHexString(
            selectApplicationResponseAdapter.apdu)).append("\n\n")

        // parsing response
        val selectApplicationParser: SelectApplicationParser = selectApplicationBuilder
          .createResponseParser(selectApplicationResponseAdapter)

        // checking response successfulness
        selectApplicationParser.checkStatus()

        // preparing command to unlock sam
        val samUnlockBuilder = SamUnlockBuilder(
          ByteConvertStringUtil.stringToByteArray(CP_SAM_UNLOCK_STRING))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samUnlockBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter
        val samUnlockResponseAdapter = ApduResponseAdapter(transmitToSam(samUnlockBuilder.apdu))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samUnlockResponseAdapter.apdu)).append("\n\n")

        // parsing response
        val samUnlockParser: SamUnlockParser = samUnlockBuilder.createResponseParser(samUnlockResponseAdapter)


        try {
          // checking response successfulness
          samUnlockParser.checkStatus()
        } catch (e: CalypsoApduCommandException) {
          if (e is CalypsoSamAccessForbiddenException) ; else throw e
        }

        // preparing select diversifier command from sam

        // preparing select diversifier command from sam
        val samSelectDiversifierBuilder = SamSelectDiversifierBuilder(
          selectApplicationParser.applicationSN)

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samSelectDiversifierBuilder.apdu)).append("\n\n")

        // // launching command and putting response into an adapter

        // // launching command and putting response into an adapter
        val samSelectDiversifierResponseAdapter = ApduResponseAdapter(transmitToSam(
          samSelectDiversifierBuilder.apdu))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samSelectDiversifierResponseAdapter.apdu)).append("\n\n")

        // parsing response

        // parsing response
        val samSelectDiversifierParser = samSelectDiversifierBuilder
          .createResponseParser(samSelectDiversifierResponseAdapter)

        // checking response successfulness

        // checking response successfulness
        samSelectDiversifierParser.checkStatus()

        // preparing command to get challenge from sam

        // preparing command to get challenge from sam
        val samGetChallengeBuilder = SamGetChallengeBuilder(Calypso.SAM_CHALLENGE_LENGTH_BYTES)

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samGetChallengeBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter

        // launching command and putting response into an adapter
        val samGetChallengeAdapter = ApduResponseAdapter(
          transmitToSam(samGetChallengeBuilder.apdu))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samGetChallengeAdapter.apdu)).append("\n\n")

        // parsing response

        // parsing response
        val samGetChallengeParser = samGetChallengeBuilder
          .createResponseParser(samGetChallengeAdapter)

        // checking response successfulness

        // checking response successfulness
        samGetChallengeParser.checkStatus()

        // preparing to open session select EF Environment file

        // preparing to open session select EF Environment file
        val cardOpenSession3Builder = CardOpenSession3Builder(0x01.toByte(),
          samGetChallengeParser.challenge, 7, 1)

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(cardOpenSession3Builder.apdu)).append("\n\n")

        // launching command and putting response into an adapter

        // launching command and putting response into an adapter
        val openSessionAdapter = ApduResponseAdapter(
          transmitToCard(cardOpenSession3Builder.apdu))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(openSessionAdapter.apdu)).append("\n\n")

        // parsing response

        // parsing response
        val openSession3Parser = cardOpenSession3Builder.createResponseParser(openSessionAdapter)

        // checking response successfulness

        // checking response successfulness
        openSession3Parser.checkStatus()

        // initializing sam digest with open session response

        // initializing sam digest with open session response
        val samDigestInitBuilder = SamDigestInitBuilder(false, false,
          openSession3Parser.selectedKif,
          openSession3Parser.selectedKvc,
          openSession3Parser.response.dataOut)

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samDigestInitBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter

        // launching command and putting response into an adapter
        val samDigestInitResponseAdapter = ApduResponseAdapter(
          transmitToSam(samDigestInitBuilder.apdu))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samDigestInitResponseAdapter.apdu)).append("\n\n")

        // parsing response

        // parsing response
        val samDigestInitParser = samDigestInitBuilder
          .createResponseParser(samDigestInitResponseAdapter)

        // checking response successfulness

        // checking response successfulness
        samDigestInitParser.checkStatus()


        // preparing to update EF Environment data
        val cardUpdateRecordBuilder = CardUpdateRecordBuilder(Calypso.SFI_EF_ENVIRONMENT.toByte(),
          1, newRecord)

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(cardUpdateRecordBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter
        val cardUpdateAdapter = ApduResponseAdapter(
          transmitToCard(cardUpdateRecordBuilder.apdu))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(cardUpdateAdapter.apdu)).append("\n\n")

        // parsing response
        val cardUpdateRecordParser = cardUpdateRecordBuilder
          .createResponseParser(cardUpdateAdapter)

        // checking response successfulness
        cardUpdateRecordParser.checkStatus()

        // preparing to update digest with update record command
        val samDigestUpdateCardWriteRequestBuilder = SamDigestUpdateBuilder(false,
          cardUpdateRecordBuilder.apdu)

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samDigestUpdateCardWriteRequestBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter
        val samDigestUpdateCardWriteRequestResponseAdapter = ApduResponseAdapter(
          transmitToSam(samDigestUpdateCardWriteRequestBuilder.apdu))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samDigestUpdateCardWriteRequestResponseAdapter.apdu)).append("\n\n")

        // parsing response
        val samDigestUpdateCardWriteRequestParser = samDigestUpdateCardWriteRequestBuilder.createResponseParser(
          samDigestUpdateCardWriteRequestResponseAdapter)

        // checking response successfulness
        samDigestUpdateCardWriteRequestParser.checkStatus()

        // preparing to update digest with update record response
        val samDigestUpdateCardWriteResponseBuilder = SamDigestUpdateBuilder(false,
          cardUpdateRecordParser.response.apdu)

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samDigestUpdateCardWriteResponseBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter
        val samDigestUpdateCardWriteResponseResponseAdapter = ApduResponseAdapter(transmitToSam(
          samDigestUpdateCardWriteResponseBuilder.apdu
        ))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samDigestUpdateCardWriteResponseResponseAdapter.apdu)).append("\n\n")

        // parsing response
        val samDigestUpdateCardWriteResponseParser = samDigestUpdateCardWriteResponseBuilder.createResponseParser(
          samDigestUpdateCardWriteResponseResponseAdapter)

        // checking response successfulness
        samDigestUpdateCardWriteResponseParser.checkStatus()

        // preparing to close digest
        val samDigestCloseBuilder = SamDigestCloseBuilder(Calypso.SAM_DIGEST_CLOSE_EXPECTED_LENGTH)

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samDigestCloseBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter
        val samDigestCloseResponseAdapter = ApduResponseAdapter(transmitToSam(samDigestCloseBuilder.apdu))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samDigestCloseResponseAdapter.apdu)).append("\n\n")

        // parsing response
        val samDigestCloseParser = samDigestCloseBuilder
          .createResponseParser(samDigestCloseResponseAdapter)

        // checking response successfulness
        samDigestCloseParser.checkStatus()

        // preparing close session command
        val cardCloseSessionBuilder = CardCloseSessionBuilder(true,
          samDigestCloseParser.signature)

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(cardCloseSessionBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter
        val cardCloseSessionResponseAdapter = ApduResponseAdapter(
          transmitToCard(cardCloseSessionBuilder.apdu))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(cardCloseSessionResponseAdapter.apdu)).append("\n\n")

        // parsing response
        val cardCloseSessionParser = cardCloseSessionBuilder
          .createResponseParser(cardCloseSessionResponseAdapter)

        // checking response successfulness
        cardCloseSessionParser.checkStatus()

        // preparing to check authenticity of close session response
        val samDigestAuthenticateBuilder = SamDigestAuthenticateBuilder(cardCloseSessionParser.signatureLo)

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samDigestAuthenticateBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter
        val samDigestAuthenticateResponseAdapter = ApduResponseAdapter(transmitToSam(samDigestAuthenticateBuilder.apdu))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samDigestAuthenticateResponseAdapter.apdu)).append("\n\n")

        // parsing response
        val samDigestAuthenticateParser = samDigestAuthenticateBuilder.createResponseParser(
          samDigestAuthenticateResponseAdapter)

        // checking response successfulness
        samDigestAuthenticateParser.checkStatus()


        promise.resolve(true)

    } catch (e: Exception) {
      promise.reject(e)
    } finally {
      disconnectSam()
      disconnectCard()
      closeCardReader()
    }
  }

  private fun connectSam() {
    openSamReader()
    try {
      (samCard as CPUCard).connect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      closeSamReader()
      throw e
    }
  }

  private fun disconnectSam() {
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

  private fun connectCard() {
    try {
      (rfCard as CPUCard).connect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      throw e
    }
  }

  private fun disconnectCard() {
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
