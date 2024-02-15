package com.pos

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.pos.byteUtils.ByteConvertReactNativeUtil
import com.pos.byteUtils.ByteConvertStringUtil
import com.pos.calypso.*


abstract class CardManager {
  protected lateinit var samId: String
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
        val map = Arguments.createMap()
        map.putString("samId", samId)
        map.putString("cardId", cardId)
        promise.resolve(map)
      } else {
        promise.reject(PosException(PosException.CARD_NOT_CONNECTED, "Card not connected"))
      }
    } catch (e: Throwable) {
      promise.reject(PosException(PosException.CARD_NOT_PRESENT, "Card not present"))
    } finally {
      disconnectCard()
    }
  }

  open suspend fun readRecordsFromCard(options: ReadableArray, promise: Promise) {
    try {
      waitForCard()
      connectCard()

      read(options, promise)
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

  open suspend fun writeToCardUpdate(options: ReadableArray, promise: Promise) {
    try {
      connectSam()
      waitForCard()
      connectCard()

      write(options, promise)
    } catch (e: Exception) {
      when (e) {
        is PosException -> promise.reject(e.code, e.message)
        else -> promise.reject(PosException.UNKNOWN, "Unknown")
      }
    } finally {
      disconnectSam()
      disconnectCard()
    }
  }

  // region Unsafe methods for custom workflows

  protected fun read(options: ReadableArray, promise: Promise) {
    val readableArray = Arguments.createArray()
    val readableMap =  Arguments.createMap()
    readableMap.putString("cardId", cardId)
    readableMap.putString("samId", samId)

    options.toArrayList().forEach {
      it as HashMap<*, *>
      val application = it["application"] as ArrayList<Int>
      val sfi = (it["sfi"] as Double).toInt()
      val offset = (it["offset"] as Double).toInt()
      val readMode = when ((it["readMode"] as Double).toInt()) {
        1 -> CardReadRecordsBuilder.ReadMode.MULTIPLE_RECORD
        else -> CardReadRecordsBuilder.ReadMode.ONE_RECORD
      }

      selectApplication(application.map { it.toByte() }.toByteArray())
      selectFileBuilder()
      val readRecordsParser = readRecordsFromSfi(sfi, offset, readMode)

      val records: Map<Int, ByteArray> = readRecordsParser.records

      val resultMap = Arguments.createMap()
      val recordsMap = Arguments.createMap()

      for ((key, record) in records) {
        val array = ByteConvertReactNativeUtil.byteArrayToReadableArray(record)
        recordsMap.putArray(key.toString(), array)
      }

      resultMap.putInt("sfi", sfi)
      resultMap.putMap("records", recordsMap)

      readableArray.pushMap(resultMap)
    }

    readableMap.putArray("data", readableArray)
    promise.resolve(readableMap)
  }

  protected fun write(options: ReadableArray, promise: Promise) {
    options.toArrayList().forEach {
      it as HashMap<*, *>
      val apdu = it["apdu"] as ArrayList<Int>
      val application = it["application"] as ArrayList<Int>
      val sfi = (it["sfi"] as Double).toInt()
      val offset = (it["offset"] as Double).toInt()
      val samUnlockString = it["samUnlockString"] as String

      val newRecord = apdu.map { it.toByte() }.toByteArray();

      val selectApplicationParser = selectApplication(ByteConvertReactNativeUtil.arrayListToByteArray(application))
      unlockSam(samUnlockString)
      selectSamDiversifier(selectApplicationParser)
      val samChallengeParser = samChallenge()
      val openSession3Parser = openSession3(samChallengeParser, sfi, offset)
      samDigestInit(openSession3Parser)
      updateRecord(sfi, offset, newRecord)
      val samDigestCloseParser = samDigestClose()
      val closeSession3Parser = closeSession3(samDigestCloseParser)
      samDigestAuthenticate(closeSession3Parser)
    }

    promise.resolve(true)
  }

  open fun unsafeConnectSam() {
    connectSam()
  }

  open suspend fun unsafeWaitForCard(promise: Promise) {
    waitForCard()
  }

  open fun unsafeConnectCard() {
    connectCard()
  }

  open fun unsafeRead(options: ReadableArray, promise: Promise) {
    read(options, promise)
  }

  open fun unsafeWrite(options: ReadableArray, promise: Promise) {
    write(options, promise)
  }

  open fun unsafeDisconnectSam() {
    disconnectSam()
  }

  open fun unsafeDisconnectCard() {
    disconnectCard()
  }

  // endregion

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
      // TODO: what is 'bytes'?
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
      throw PosException(PosException.SAM_CONNECT_FAIL, "Can't unlock SAM")
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
