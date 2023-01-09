package com.pos

import com.cloudpos.*
import com.cloudpos.card.CPUCard
import com.cloudpos.card.Card
import com.cloudpos.rfcardreader.RFCardReaderDevice
import com.cloudpos.smartcardreader.SmartCardReaderDevice
import com.cloudpos.smartcardreader.SmartCardReaderOperationResult
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.pos.byte_stuff.ByteConvertStringUtil
import com.pos.calypso.Calypso
import com.pos.calypso.SamGetChallengeBuilder

class Famoco(private val reactContext: ReactApplicationContext): com.pos.Card() {

  private lateinit var samId: String
  private lateinit var samCard: Card

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
      famocoSmartCardReaderDevice.open()
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
      initialized(true)
    }
  }

  override fun init(promise: Promise) {
    this.promise = promise;
    open()
  }

  override fun initialized(status: Boolean) {
    if (status) {
      val challengeApdu = getChallengeApdu()
      val response = transmitToSam(challengeApdu)

      val array = Arguments.createArray()
      response?.forEach {
        array.pushInt(it.toInt())
      }
      promise?.resolve(array)
      promise = null

      close()
    } else {
      promise?.reject("Initialization failed")
      promise = null
    }
  }

  override fun writeToCard(apdu: ReadableArray, promise: Promise) {
    if (this.promise == null) {
      this.apdu = (apdu.toArrayList() as ArrayList<Int>).map { it.toByte() }.toByteArray();
      this.promise = promise;
      // TODO: add code for write
      this.promise?.resolve(null)
      this.promise = null
    } else {
      promise.reject("Write/read to card in progress")
    }
  }

  override fun getChallengeApdu(): ByteArray {
    val samGetChallengeBuilder = SamGetChallengeBuilder(Calypso.SAM_CHALLENGE_LENGTH_BYTES)
    val apdu = samGetChallengeBuilder.apdu
    return apdu;
  }

  override fun transmitToSam(apdu: ByteArray): ByteArray? {
    return try {
      (samCard as CPUCard).connect()
      val response = (samCard as CPUCard).transmit(apdu)
      response
    } catch (e: NullPointerException) {
      null
    }
  }

  override fun transmitToCard(apdu: ByteArray): ByteArray? {
    TODO("Not yet implemented")
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
