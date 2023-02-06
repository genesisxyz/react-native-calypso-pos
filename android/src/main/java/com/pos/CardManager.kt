package com.pos

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.pos.byteUtils.ByteConvertReactNativeUtil
import com.pos.byteUtils.ByteConvertStringUtil
import com.pos.calypso.*


abstract class CardManager {
  protected lateinit var cardId: String

  protected var samIsConnected = false
  protected var cardIsConnected = false

  abstract suspend fun init(promise: Promise)

  abstract fun close()

  protected abstract fun transmitToSam(apdu: ByteArray): ByteArray?

  protected abstract fun transmitToCard(apdu: ByteArray): ByteArray?

  protected abstract fun connectSam()

  protected abstract fun disconnectSam()

  protected abstract fun connectCard()

  protected abstract fun disconnectCard()

  protected abstract suspend fun waitForCard()

  protected fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  private fun readRecordsFromSfi(sfi: Int, offset: Int, readMode: CardReadRecordsBuilder.ReadMode): CardReadRecordsParser {
    val readRecordsBuilder = CardReadRecordsBuilder(sfi, offset, readMode, 0)

    val readRecordsResponseAdapter = ApduResponseAdapter(transmitToCard(readRecordsBuilder.apdu))

    val readRecordsParser = readRecordsBuilder.createResponseParser(readRecordsResponseAdapter)

    readRecordsParser.checkStatus()

    return readRecordsParser
  }

  open suspend fun readCardId(promise: Promise) {
    try {
      waitForCard()
      connectCard()
      if (cardIsConnected) {
        promise.resolve(cardId)
      } else {
        promise.reject(PosException(PosException.CARD_NOT_PRESENT, "Card not connected"))
      }
    } catch (e: Throwable) {
      promise.reject(PosException(PosException.CARD_NOT_PRESENT, "Card not present"))
    } finally {
      disconnectCard()
    }
  }

  open suspend fun readRecordsFromCard(options: ReadableMap, promise: Promise) {
    val application = options.getArray("application")!!
    val sfi = options.getInt("sfi")
    val offset = options.getInt("offset")
    // val readMode = CardReadRecordsBuilder.ReadMode.values().first { it.ordinal == options.getInt("readMore") }
    val readMode = CardReadRecordsBuilder.ReadMode.ONE_RECORD;

    try {
      waitForCard()
      connectCard()

      selectApplication(ByteConvertReactNativeUtil.readableArrayToByteArray(application))
      selectFileBuilder()
      val readRecordsParser = readRecordsFromSfi(sfi, offset, readMode)

      val records: Map<Int, ByteArray> = readRecordsParser.records

      val readableMap = Arguments.createMap()
      val recordsMap = Arguments.createMap()

      for ((key, record) in records) {
        val array = ByteConvertReactNativeUtil.byteArrayToReadableArray(record)
        recordsMap.putArray(key.toString(), array)
      }

      readableMap.putMap("records", recordsMap)
      readableMap.putString("cardId", cardId)

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
    }
  }

  open suspend fun writeToCardUpdate(apdu: ReadableArray, options: ReadableMap, promise: Promise) {
    val application = options.getArray("application")!!;
    val sfi = options.getInt("sfi")
    val offset = options.getInt("offset")
    val samUnlockString = options.getString("samUnlockString")!!

    try {
      connectSam()
      waitForCard()
      connectCard()

      val newRecord = (apdu.toArrayList() as ArrayList<Int>).map { it.toByte() }.toByteArray();

      val selectApplicationParser = selectApplication(ByteConvertReactNativeUtil.readableArrayToByteArray(application))
      unlockSam(samUnlockString)
      selectSamDiversifier(selectApplicationParser)
      val samChallengeParser = samChallenge()
      val openSession3Parser = openSession3(samChallengeParser, sfi, offset)
      samDigestInit(openSession3Parser)
      updateRecord(sfi, offset, newRecord)
      val samDigestCloseParser = samDigestClose()
      val closeSession3Parser = closeSession3(samDigestCloseParser)
      samDigestAuthenticate(closeSession3Parser)

      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject(e)
    } finally {
      disconnectSam()
      disconnectCard()
    }
  }

  private fun selectApplication(application: ByteArray): SelectApplicationParser {
    val selectApplicationBuilder = SelectApplicationBuilder(
      SelectApplicationBuilder.SELECT_FIRST_OCCURRENCE_RETURN_FCI, application)

    val selectApplicationResponseAdapter = ApduResponseAdapter(transmitToCard(selectApplicationBuilder.apdu))

    val selectApplicationParser = selectApplicationBuilder.createResponseParser(selectApplicationResponseAdapter)

    selectApplicationParser.checkStatus()

    return selectApplicationParser
  }

  private fun selectFileBuilder(): CardSelectFileParser {
    val selectFileBuilder = CardSelectFileBuilder(Calypso.LID_EF_ENVIRONMENT)

    val selectFileResponseAdapter = ApduResponseAdapter(transmitToCard(selectFileBuilder.apdu))

    val selectFileParser = selectFileBuilder
      .createResponseParser(selectFileResponseAdapter)

    selectFileParser.checkStatus()

    if (!selectFileParser.isSuccess || selectFileParser.proprietaryInformation == null) {
      throw PosException(PosException.CARD_NOT_SUPPORTED, "Card not supported")
    }

    return selectFileParser
  }

  private fun unlockSam(samUnlockString: String): SamUnlockParser {
    try {
      val bytes = ByteConvertStringUtil.stringToByteArray(samUnlockString)
      val samUnlockBuilder = SamUnlockBuilder(ByteConvertStringUtil.stringToByteArray(samUnlockString))
      val samUnlockResponseAdapter = ApduResponseAdapter(transmitToSam(samUnlockBuilder.apdu))
      val samUnlockParser = samUnlockBuilder.createResponseParser(samUnlockResponseAdapter)
      // TODO: why this try catch?
      try {
        samUnlockParser.checkStatus()
      } catch (e: CalypsoApduCommandException) {
        if (e is CalypsoSamAccessForbiddenException) ; else throw e
      }

      return samUnlockParser
    } catch (e: PosException) {
      throw e
    }
  }

  private fun selectSamDiversifier(selectApplicationParser: SelectApplicationParser): SamSelectDiversifierParser {
    val samSelectDiversifierBuilder = SamSelectDiversifierBuilder(
      selectApplicationParser.applicationSN)

    val samSelectDiversifierResponseAdapter = ApduResponseAdapter(transmitToSam(
      samSelectDiversifierBuilder.apdu))

    val samSelectDiversifierParser = samSelectDiversifierBuilder
      .createResponseParser(samSelectDiversifierResponseAdapter)

    samSelectDiversifierParser.checkStatus()

    return samSelectDiversifierParser
  }

  private fun samChallenge(): SamGetChallengeParser {
    val samGetChallengeBuilder = SamGetChallengeBuilder(Calypso.SAM_CHALLENGE_LENGTH_BYTES)

    val samGetChallengeAdapter = ApduResponseAdapter(transmitToSam(samGetChallengeBuilder.apdu))

    val samGetChallengeParser = samGetChallengeBuilder.createResponseParser(samGetChallengeAdapter)

    samGetChallengeParser.checkStatus()

    return samGetChallengeParser
  }

  private fun openSession3(samGetChallengeParser: SamGetChallengeParser, sfi: Int, offset: Int): CardOpenSession3Parser {
    // TODO: first parameter; CP: 1, CL: 2, CV: 3
    val cardOpenSession3Builder = CardOpenSession3Builder(0x01.toByte(), samGetChallengeParser.challenge, sfi, offset)

    val openSessionAdapter = ApduResponseAdapter(transmitToCard(cardOpenSession3Builder.apdu))

    val openSession3Parser = cardOpenSession3Builder.createResponseParser(openSessionAdapter)

    openSession3Parser.checkStatus()

    return openSession3Parser
  }

  private fun samDigestInit(openSession3Parser: CardOpenSession3Parser) {
    val samDigestInitBuilder = SamDigestInitBuilder(false, false,
      openSession3Parser.selectedKif,
      openSession3Parser.selectedKvc,
      openSession3Parser.response.dataOut)

    val samDigestInitResponseAdapter = ApduResponseAdapter(
      transmitToSam(samDigestInitBuilder.apdu))

    val samDigestInitParser = samDigestInitBuilder
      .createResponseParser(samDigestInitResponseAdapter)

    samDigestInitParser.checkStatus()
  }

  private fun updateRecord(sfi: Int, offset: Int, record: ByteArray) {
    val cardUpdateRecordBuilder = CardUpdateRecordBuilder(sfi.toByte(), offset, record)

    val cardUpdateAdapter = ApduResponseAdapter(
      transmitToCard(cardUpdateRecordBuilder.apdu))

    val cardUpdateRecordParser = cardUpdateRecordBuilder
      .createResponseParser(cardUpdateAdapter)

    cardUpdateRecordParser.checkStatus()

    val samDigestUpdateCardWriteRequestBuilder = SamDigestUpdateBuilder(false,
      cardUpdateRecordBuilder.apdu)

    val samDigestUpdateCardWriteRequestResponseAdapter = ApduResponseAdapter(
      transmitToSam(samDigestUpdateCardWriteRequestBuilder.apdu))

    val samDigestUpdateCardWriteRequestParser = samDigestUpdateCardWriteRequestBuilder.createResponseParser(
      samDigestUpdateCardWriteRequestResponseAdapter)

    samDigestUpdateCardWriteRequestParser.checkStatus()

    val samDigestUpdateCardWriteResponseBuilder = SamDigestUpdateBuilder(false,
      cardUpdateRecordParser.response.apdu)

    val samDigestUpdateCardWriteResponseResponseAdapter = ApduResponseAdapter(transmitToSam(
      samDigestUpdateCardWriteResponseBuilder.apdu
    ))

    val samDigestUpdateCardWriteResponseParser = samDigestUpdateCardWriteResponseBuilder.createResponseParser(
      samDigestUpdateCardWriteResponseResponseAdapter)

    samDigestUpdateCardWriteResponseParser.checkStatus()
  }

  private fun samDigestClose(): SamDigestCloseParser {
    val samDigestCloseBuilder = SamDigestCloseBuilder(Calypso.SAM_DIGEST_CLOSE_EXPECTED_LENGTH)

    val samDigestCloseResponseAdapter = ApduResponseAdapter(transmitToSam(samDigestCloseBuilder.apdu))

    val samDigestCloseParser = samDigestCloseBuilder
      .createResponseParser(samDigestCloseResponseAdapter)

    samDigestCloseParser.checkStatus()

    return samDigestCloseParser
  }

  private fun closeSession3(samDigestCloseParser: SamDigestCloseParser): CardCloseSessionParser {
    val cardCloseSessionBuilder = CardCloseSessionBuilder(true,
      samDigestCloseParser.signature)

    val cardCloseSessionResponseAdapter = ApduResponseAdapter(
      transmitToCard(cardCloseSessionBuilder.apdu))

    val cardCloseSessionParser = cardCloseSessionBuilder
      .createResponseParser(cardCloseSessionResponseAdapter)

    cardCloseSessionParser.checkStatus()

    return cardCloseSessionParser
  }

  private fun samDigestAuthenticate(closeSessionParser: CardCloseSessionParser) {
    val samDigestAuthenticateBuilder = SamDigestAuthenticateBuilder(closeSessionParser.signatureLo)

    val samDigestAuthenticateResponseAdapter = ApduResponseAdapter(transmitToSam(samDigestAuthenticateBuilder.apdu))

    val samDigestAuthenticateParser = samDigestAuthenticateBuilder.createResponseParser(
      samDigestAuthenticateResponseAdapter)

    samDigestAuthenticateParser.checkStatus()
  }

  companion object {
    const val TAG = "CardManager"
  }
}
