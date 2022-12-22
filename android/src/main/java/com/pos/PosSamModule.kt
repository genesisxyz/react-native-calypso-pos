package com.pos

import android.os.Build
import com.cloudpos.*
import com.cloudpos.card.CPUCard
import com.cloudpos.card.Card
import com.cloudpos.smartcardreader.SmartCardReaderDevice
import com.cloudpos.smartcardreader.SmartCardReaderOperationResult
import com.facebook.react.bridge.*
import com.pos.byte_stuff.ByteConvertStringUtil
import com.pos.calypso.Calypso
import com.pos.calypso.SamGetChallengeBuilder
import com.telpo.tps550.api.reader.SmartCardReader

class PosSamModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  // variabili globali
  private val famocoSamLogicalID = 1
  private val famocoSmartCardReaderDevice: SmartCardReaderDevice = POSTerminal.getInstance(reactApplicationContext).getDevice("cloudpos.device.smartcardreader", famocoSamLogicalID) as SmartCardReaderDevice

  private lateinit var samId: String
  private lateinit var samCard: Card
  private lateinit var samReader: SmartCardReader

  private val isFamoco = Build.MANUFACTURER.equals("wizarPOS")

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun open(promise: Promise){
    if(isFamoco){
      try {
        famocoSmartCardReaderDevice.open()
        val listener = OperationListener { arg0: OperationResult ->
          if (arg0.resultCode == OperationResult.SUCCESS) {
            samCard = (arg0 as SmartCardReaderOperationResult).card
            try {
              val tempSamId: String = ByteConvertStringUtil.bytesToHexString(samCard.id)
              if (tempSamId != null) samId = tempSamId.substring(24, 32)
              promise.resolve(true)
            } catch (e: DeviceException) {
              e.printStackTrace()
              promise.resolve(false)
            }
          }
        }
        famocoSmartCardReaderDevice.listenForCardPresent(listener, TimeConstants.FOREVER)
      } catch (e: DeviceException){
        promise.resolve(false)
      }
    }else{
      try {
        samReader = SmartCardReader(reactApplicationContext, SmartCardReader.SLOT_PSAM2)
        samReader.open()
        val isPowerOn = samReader.iccPowerOn()
        if(isPowerOn) {
          val atrString = samReader.atrString
          if (atrString !== null) {
            samId = atrString.substring(24, 32)
            promise.resolve(true)
            return
          }
        }
        promise.resolve(false)
      }
      catch (e: Throwable){
        promise.resolve(false)
      }
    }
  }

  @ReactMethod
  fun getManufacturer(promise: Promise){
    promise.resolve(Build.MANUFACTURER)
  }

  @ReactMethod
  fun close(promise: Promise){
    if(isFamoco) {
      try {
        famocoSmartCardReaderDevice.close()
        promise.resolve(true)
      } catch (e: DeviceException) {
        promise.resolve(false)
      }
    }else{
      promise.resolve(samReader.close())
    }
  }

  @ReactMethod
  fun transmit(command: ReadableArray, promise: Promise) {
    if (isFamoco){
      try {
        val test = command.toArrayList() as ArrayList<Int>
        val test2 = test.map { it.toByte() }.toByteArray()
        (samCard as CPUCard).connect()
        val bytes = (samCard as CPUCard).transmit(test2)
        val array = Arguments.createArray()
        bytes.forEach {
          array.pushInt(it.toInt())
        }
        promise.resolve(array)
      } catch (e: NullPointerException) {
        promise.resolve(arrayOf<Any>())
      }
    }else{
      try {
        val test = command.toArrayList() as ArrayList<Int>
        val test2 = test.map { it.toByte() }.toByteArray()
        val bytes = samReader.transmit(test2)
        val array = Arguments.createArray()
        bytes.forEach {
          array.pushInt(it.toInt())
        }
        promise.resolve(array)
      }
      catch (e: NullPointerException){
        promise.resolve(e)
      }
    }
  }

  @ReactMethod
  fun challenge(promise: Promise) {
    if(isFamoco){
      val samGetChallengeBuilder = SamGetChallengeBuilder(Calypso.SAM_CHALLENGE_LENGTH_BYTES)
      val apdu = samGetChallengeBuilder.apdu
      val array = Arguments.createArray()
      apdu.forEach {
        array.pushInt(it.toInt())
      }
      promise.resolve(array)
    }else{
      val samGetChallengeBuilder = SamGetChallengeBuilder(Calypso.SAM_CHALLENGE_LENGTH_BYTES)
      val apdu = samGetChallengeBuilder.apdu
      val array = Arguments.createArray()
      apdu.forEach {
        array.pushInt(it.toInt())
      }
      promise.resolve(array)
    }
  }

  companion object {
    const val NAME = "PosSam"
  }
}
