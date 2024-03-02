package com.pos

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.pos.byteUtils.ByteConvertReactNativeUtil
import com.pos.byteUtils.ByteConvertStringUtil
import com.pos.calypso.*
import okhttp3.internal.toHexString


abstract class CardManager {
  // TODO: should samId be nullable?
  protected var samId: String? = null
  protected lateinit var cardId: String

  protected var samIsConnected = false
  protected var cardIsConnected = false

  abstract suspend fun init(promise: Promise)

  abstract suspend fun close()

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

  open fun getSamId(promise: Promise) {
    try {
      connectSam()

      val response = Arguments.createMap()
      response.putString("samId", samId)
      promise.resolve(response)
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

  open fun samComputeEventLogSignature(options: ReadableMap, promise: Promise) {
    val userInfo = Arguments.createMap()
    userInfo.putBoolean("isPosError", true)

    if (!samIsConnected) {
      promise.reject(PosException.NO_SAM_AVAILABLE, "No SAM available", userInfo)
      return
    }

    try {
      val kif = options.getInt("kif").toByte()
      val kvc = options.getInt("kvc").toByte()
      val log = ByteConvertReactNativeUtil.arrayListToByteArray(options.getArray("log")!!.toArrayList() as ArrayList<Int>)
      val samUnlockString = options.getString("samUnlockString")!!

      unlockSam(samUnlockString)

      val cardSerialNumber = ByteConvertStringUtil.stringToByteArray("00000000${cardId.toLong().toHexString()}")

      val samComputeLogSignatureBuilder = SamComputeLogSignatureBuilder(kif,
        kvc, cardSerialNumber, log)
      val samComputeLogSignatureResponseAdapter = ApduResponseAdapter(
        transmitToSam(samComputeLogSignatureBuilder.apdu))
      val samComputeLogSignatureParser = samComputeLogSignatureBuilder.createResponseParser(
        samComputeLogSignatureResponseAdapter)

      samComputeLogSignatureParser.checkStatus()

      promise.resolve(ByteConvertReactNativeUtil.byteArrayToReadableArray(samComputeLogSignatureParser.signature))
    } catch (e: Exception) {
      val userInfo = Arguments.createMap()
      userInfo.putBoolean("isPosError", true)
      when (e) {
        is PosException -> promise.reject(e.code, e.message, userInfo)
        else -> promise.reject(PosException.UNKNOWN, "Unknown", userInfo)
      }
    }
  }

  // region Unsafe methods for custom workflows

  protected fun read(options: ReadableArray, promise: Promise) {
    val readableArray = Arguments.createArray()

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

    promise.resolve(readableArray)
  }

  protected open fun write(options: ReadableArray, promise: Promise) {
    connectSam()

    options.toArrayList().forEach {
      it as HashMap<*, *>
      val apdu = it["apdu"] as ArrayList<Int>
      val application = it["application"] as ArrayList<Int>
      val sfi = (it["sfi"] as Double).toInt()
      val offset = (it["offset"] as Double?)?.toInt()
      val samUnlockString = it["samUnlockString"] as String

      val newRecord = apdu.map { it.toByte() }.toByteArray();

      val selectApplicationParser = selectApplication(ByteConvertReactNativeUtil.arrayListToByteArray(application))
      unlockSam(samUnlockString)
      selectSamDiversifier(selectApplicationParser)
      val samChallengeParser = samChallenge()
      val openSession3Parser = openSession3(samChallengeParser, sfi, if (offset !== null) offset else 1)
      samDigestInit(openSession3Parser)
      updateRecord(sfi, offset, newRecord)
      val samDigestCloseParser = samDigestClose()
      val closeSession3Parser = closeSession3(samDigestCloseParser)
      samDigestAuthenticate(closeSession3Parser)
    }

    disconnectSam()

    promise.resolve(true)
  }

  open suspend fun unsafeWaitForCard(promise: Promise) {
    try {
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

  open fun unsafeConnectCard(promise: Promise) {
    try {
      connectCard()
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

  open fun unsafeRead(options: ReadableArray, promise: Promise) {
    try {
      read(options, promise)
    } catch (e: Exception) {
      val userInfo = Arguments.createMap()
      userInfo.putBoolean("isPosError", true)
      when (e) {
        is PosException -> promise.reject(e.code, e.message, userInfo)
        else -> promise.reject(PosException.UNKNOWN, "Unknown", userInfo)
      }
    }
  }

  open fun unsafeWrite(options: ReadableArray, promise: Promise) {
    try {
      write(options, promise)
    } catch (e: Exception) {
      val userInfo = Arguments.createMap()
      userInfo.putBoolean("isPosError", true)
      when (e) {
        is PosException -> promise.reject(e.code, e.message, userInfo)
        else -> promise.reject(PosException.UNKNOWN, "Unknown", userInfo)
      }
    }
  }

  open fun unsafeDisconnectCard(promise: Promise) {
    try {
      disconnectCard()
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

  // TODO: needs refactoring
  private fun updateRecord(sfi: Int, offset: Int?, record: ByteArray) {
    if (offset == null) {
      val cardAppendRecordBuilder = CardAppendRecordBuilder(sfi.toByte(), record)

      val cardUpdateAdapter = ApduResponseAdapter(
        transmitToCard(cardAppendRecordBuilder.apdu))

      val cardUpdateRecordParser = cardAppendRecordBuilder
        .createResponseParser(cardUpdateAdapter)

      cardUpdateRecordParser.checkStatus()
      val samDigestUpdateCardWriteRequestBuilder = SamDigestUpdateBuilder(false,
        cardAppendRecordBuilder.apdu)

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
    } else {
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
