package com.pos

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableArray
import com.pos.byteUtils.ByteConvertStringUtil
import com.pos.calypso.*

abstract class CardManager {
  abstract suspend fun init(promise: Promise)

  protected abstract fun transmitToSam(apdu: ByteArray): ByteArray?

  protected abstract fun transmitToCard(apdu: ByteArray): ByteArray?

  protected abstract fun connectSam()

  protected abstract fun disconnectSam()

  protected abstract fun connectCard()

  protected abstract fun disconnectCard()

  protected abstract suspend fun waitForCard()

  private val CP_SAM_UNLOCK_STRING: String = "62 EE D0 33 FB 9F D1 85 B3 C7 DA BD 02 82 D6 EC"

  abstract suspend fun readRecordsFromCard(promise: Promise)

  open suspend fun writeToCard(apdu: ReadableArray, promise: Promise) {
    try {
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
      val samSelectDiversifierBuilder = SamSelectDiversifierBuilder(
        selectApplicationParser.applicationSN)

      commandsSequenceBuilder.append(ByteConvertStringUtil
        .bytesToHexString(samSelectDiversifierBuilder.apdu)).append("\n\n")

      // // launching command and putting response into an adapter
      val samSelectDiversifierResponseAdapter = ApduResponseAdapter(transmitToSam(
        samSelectDiversifierBuilder.apdu))

      commandsSequenceBuilder.append(ByteConvertStringUtil
        .bytesToHexString(samSelectDiversifierResponseAdapter.apdu)).append("\n\n")

      // parsing response
      val samSelectDiversifierParser = samSelectDiversifierBuilder
        .createResponseParser(samSelectDiversifierResponseAdapter)

      // checking response successfulness
      samSelectDiversifierParser.checkStatus()

      // preparing command to get challenge from sam
      val samGetChallengeBuilder = SamGetChallengeBuilder(Calypso.SAM_CHALLENGE_LENGTH_BYTES)

      commandsSequenceBuilder.append(ByteConvertStringUtil
        .bytesToHexString(samGetChallengeBuilder.apdu)).append("\n\n")

      // launching command and putting response into an adapter
      val samGetChallengeAdapter = ApduResponseAdapter(
        transmitToSam(samGetChallengeBuilder.apdu))

      commandsSequenceBuilder.append(ByteConvertStringUtil
        .bytesToHexString(samGetChallengeAdapter.apdu)).append("\n\n")

      // parsing response
      val samGetChallengeParser = samGetChallengeBuilder
        .createResponseParser(samGetChallengeAdapter)

      // checking response successfulness
      samGetChallengeParser.checkStatus()

      // preparing to open session select EF Environment file
      val cardOpenSession3Builder = CardOpenSession3Builder(0x01.toByte(),
        samGetChallengeParser.challenge, 7, 1)

      commandsSequenceBuilder.append(ByteConvertStringUtil
        .bytesToHexString(cardOpenSession3Builder.apdu)).append("\n\n")

      // launching command and putting response into an adapter
      val openSessionAdapter = ApduResponseAdapter(
        transmitToCard(cardOpenSession3Builder.apdu))

      commandsSequenceBuilder.append(ByteConvertStringUtil
        .bytesToHexString(openSessionAdapter.apdu)).append("\n\n")

      // parsing response
      val openSession3Parser = cardOpenSession3Builder.createResponseParser(openSessionAdapter)

      // checking response successfulness
      openSession3Parser.checkStatus()

      // initializing sam digest with open session response
      val samDigestInitBuilder = SamDigestInitBuilder(false, false,
        openSession3Parser.selectedKif,
        openSession3Parser.selectedKvc,
        openSession3Parser.response.dataOut)

      commandsSequenceBuilder.append(ByteConvertStringUtil
        .bytesToHexString(samDigestInitBuilder.apdu)).append("\n\n")

      // launching command and putting response into an adapter
      val samDigestInitResponseAdapter = ApduResponseAdapter(
        transmitToSam(samDigestInitBuilder.apdu))

      commandsSequenceBuilder.append(ByteConvertStringUtil
        .bytesToHexString(samDigestInitResponseAdapter.apdu)).append("\n\n")

      // parsing response
      val samDigestInitParser = samDigestInitBuilder
        .createResponseParser(samDigestInitResponseAdapter)

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
    }
  }
}
