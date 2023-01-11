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

class Famoco(private val reactContext: ReactApplicationContext): com.pos.Card() {

  private lateinit var samId: String
  private lateinit var samCard: Card
  private lateinit var rfCard: Card

  private val famocoSamLogicalID = 1
  private lateinit var famocoRFCardReaderDevice: RFCardReaderDevice
  private lateinit var famocoSmartCardReaderDevice: SmartCardReaderDevice

  private var promise: Promise? = null
  private lateinit var apdu: ByteArray;

  override fun open() {
    try {
      famocoRFCardReaderDevice = POSTerminal.getInstance(reactContext).getDevice("cloudpos.device.rfcardreader") as RFCardReaderDevice
      famocoSmartCardReaderDevice = POSTerminal.getInstance(reactContext).getDevice("cloudpos.device.smartcardreader", famocoSamLogicalID) as SmartCardReaderDevice
    } catch (e: Throwable) {
      e.printStackTrace()
      initialized(false)
    }

    try {
      famocoSmartCardReaderDevice.open(famocoSamLogicalID)
      val listener = OperationListener { arg0: OperationResult ->
        if (arg0.resultCode == OperationResult.SUCCESS) {
          samCard = (arg0 as SmartCardReaderOperationResult).card
          try {
            val tempSamId: String = ByteConvertStringUtil.bytesToHexString(samCard.id)
            if (tempSamId != null) samId = tempSamId.substring(24, 32)
            initialized(true)
          } catch (e: DeviceException) {
            e.printStackTrace()
            initialized(false)
          }
        }
      }
      famocoSmartCardReaderDevice.listenForCardPresent(listener, TimeConstants.FOREVER)
    } catch (e: DeviceException){
      e.printStackTrace()
      initialized(false)
    }
  }

  override fun init(promise: Promise) {
    this.promise = promise;
    open()
  }

  override fun initialized(status: Boolean) {
    if (status) {
      val challengeApdu = getChallengeApdu()

      connectSam()
      val response = transmitToSam(challengeApdu)
      disconnectSam()

      // TODO: check if the response is ok
      promise?.resolve(true)
      promise = null

      close()
    } else {
      promise?.resolve(false)
      promise = null
    }
  }

  private val CP_SAM_UNLOCK_STRING: String = "62 EE D0 33 FB 9F D1 85 B3 C7 DA BD 02 82 D6 EC"

  private fun waitForCard(callback: () -> Unit) {
    try {
      famocoRFCardReaderDevice.open()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
      } else {
        // TODO device is ready
      }
    }

    var cardId: String? = null

    try {
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
        callback()
      } else {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
      }
    } catch (e: DeviceException) {
      e.printStackTrace()

      // pending request
      if (e.code == -3) {
        try {
          famocoRFCardReaderDevice.cancelRequest()
        } catch (deviceException: DeviceException) {
          deviceException.printStackTrace()
        }
      }

      throw e
    } finally {
      famocoRFCardReaderDevice.close()
    }
  }

  override fun readRecordsFromCard(promise: Promise) {
    try {
      waitForCard {

            connectCard()

            val selectApplicationBuilder = SelectApplicationBuilder(
              SelectApplicationBuilder.SELECT_FIRST_OCCURRENCE_RETURN_FCI)

            val selectApplicationResponseAdapter = ApduResponseAdapter(
              transmitToCard(selectApplicationBuilder.apdu))

            val selectApplicationParser = selectApplicationBuilder
              .createResponseParser(selectApplicationResponseAdapter)

            selectApplicationParser.checkStatus()

            if (!selectApplicationParser.isBipApplication) {
              throw PosException(PosException.CARD_NOT_SUPPORTED, "Card not supported")
            }

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

            disconnectCard()

            val readableMap = Arguments.createMap();
            for ((key, record) in records) {
              val array = ByteConvertReactNativeUtil.byteArrayToReadableArray(record)
              readableMap.putArray(key.toString(), array)
            }

            promise.resolve(readableMap)

      }
    } catch (e: Exception) {
      e.printStackTrace()
      if (e is PosException) {
        promise.reject(e)
      } else {
        promise.reject(PosException(PosException.CARD_NOT_PRESENT, "Card not present"))
      }
    }
  }

  override fun writeToCard(apdu: ReadableArray, promise: Promise) {
    if (this.promise == null) {
      this.apdu = (apdu.toArrayList() as ArrayList<Int>).map { it.toByte() }.toByteArray();
      this.promise = promise;
      // TODO: add code for write


      val commandsSequenceBuilder = StringBuilder()



      try {

        connectSam()

        // preparing select application command
        val selectApplicationBuilder = SelectApplicationBuilder(
          SelectApplicationBuilder.SELECT_FIRST_OCCURRENCE_RETURN_FCI)

        commandsSequenceBuilder.append(
          ByteConvertStringUtil.bytesToHexString(
            selectApplicationBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter

        // launching command and putting response into an adapter
        val selectApplicationResponseAdapter = ApduResponseAdapter(
          transmitToCard(selectApplicationBuilder.getApdu()))

        commandsSequenceBuilder.append(
          ByteConvertStringUtil.bytesToHexString(
            selectApplicationResponseAdapter.getApdu())).append("\n\n")

        // parsing response

        // parsing response
        val selectApplicationParser: SelectApplicationParser = selectApplicationBuilder
          .createResponseParser(selectApplicationResponseAdapter)

        // checking response successfulness

        // checking response successfulness
        selectApplicationParser.checkStatus()

        // preparing command to unlock sam

        // preparing command to unlock sam
        val samUnlockBuilder = SamUnlockBuilder(
          ByteConvertStringUtil.stringToByteArray(CP_SAM_UNLOCK_STRING))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samUnlockBuilder.apdu)).append("\n\n")

        // launching command and putting response into an adapter

        // launching command and putting response into an adapter
        val samUnlockResponseAdapter = ApduResponseAdapter(transmitToSam(samUnlockBuilder.apdu))

        commandsSequenceBuilder.append(ByteConvertStringUtil
          .bytesToHexString(samUnlockResponseAdapter.apdu)).append("\n\n")

        // parsing response

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


        // preparing data to write on EF Environment card file





      } catch (e: Exception) {

      } finally {
          disconnectSam()
        disconnectCard()
      }




      this.promise?.resolve(null)
      this.promise = null
    } else {
      promise.reject("Write/read to card in progress")
    }
  }

  override fun getChallengeApdu(): ByteArray {
    val samGetChallengeBuilder = SamGetChallengeBuilder(Calypso.SAM_CHALLENGE_LENGTH_BYTES)
    return samGetChallengeBuilder.apdu;
  }

  fun connectSam() {
    try {
      (samCard as CPUCard).connect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Sam not present")
      }
    }
  }

  fun disconnectSam() {
    try {
      (samCard as CPUCard).disconnect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Sam not present")
      }
    }
  }

  override fun transmitToSam(apdu: ByteArray): ByteArray? {
    return try {
      (samCard as CPUCard).transmit(apdu)
    } catch (e: Exception) {
      throw PosException(PosException.TRANSMIT_APDU_COMMAND, "Transmit apdu command error")
    }
  }

  fun connectCard() {
    try {
      (rfCard as CPUCard).connect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
      }
    }
  }

  fun disconnectCard() {
    try {
      (rfCard as CPUCard).disconnect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
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

  override fun close(): Boolean {
    return try {
      famocoSmartCardReaderDevice.close()
      true
    } catch (e: DeviceException) {
      false
    }
  }
}
