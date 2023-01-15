package com.pos

import android.nfc.tech.IsoDep
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.pos.byte_stuff.ByteConvertReactNativeUtil
import com.pos.calypso.*
import com.telpo.tps550.api.TelpoException
import com.telpo.tps550.api.reader.SmartCardReader
import java.io.IOException

class Telpo(private val reactContext: ReactApplicationContext): com.pos.Card() {

  private lateinit var samReader: SmartCardReader
  private lateinit var samId: String

  private var promise: Promise? = null
  private lateinit var apdu: ByteArray;

  private val isoDep: IsoDep? = null

  fun open() {
    try {
      samReader = SmartCardReader(reactContext, SmartCardReader.SLOT_PSAM2)
      samReader.open()
      val isPowerOn = samReader.iccPowerOn()
      if(isPowerOn) {
        val atrString = samReader.atrString
        if (atrString !== null) {
          samId = atrString.substring(24, 32)
          initialized(true)
          return
        }
      }
      initialized(false)
    }
    catch (e: Throwable){
      initialized(false)
    }
  }

  override suspend fun init(promise: Promise) {
    this.promise = promise;
    open()
  }

  fun initialized(status: Boolean) {
    if (status) {
      promise?.resolve(true)
      promise = null

      close()
    } else {
      promise?.resolve(false)
      promise = null
    }
  }

  override suspend fun writeToCard(apdu: ReadableArray, promise: Promise) {
    if (this.promise == null) {
      this.apdu = (apdu.toArrayList() as ArrayList<Int>).map { it.toByte() }.toByteArray();
      this.promise = promise;
      // TODO: add code for write
      this.promise?.resolve(null)
      this.promise = null
    } else {
      promise.reject("Write/read to card already in progress")
    }
  }

  override suspend fun readRecordsFromCard(promise: Promise){
    val readRecordsBuilder = CardReadRecordsBuilder(
      Calypso.SFI_EF_ENVIRONMENT, 1,
      CardReadRecordsBuilder.ReadMode.ONE_RECORD, 0)

    val readRecordsResponseAdapter = ApduResponseAdapter(transmitToCard(readRecordsBuilder.apdu))

    val readRecordsParser: CardReadRecordsParser = readRecordsBuilder.createResponseParser(readRecordsResponseAdapter)

    readRecordsParser.checkStatus()

    val records: Map<Int, ByteArray> = readRecordsParser.records
    if(records != null && records.isNotEmpty()){
      val readableMap = Arguments.createMap()
      for ((key, value) in records) {
        if (value != null) {
          readableMap.putInt(key.toString(), key)
          readableMap.putArray(value.toString(), ByteConvertReactNativeUtil.byteArrayToReadableArray(value))
        }
      }
      promise.resolve(readableMap)
    } else{
      promise.reject("records null or empty")
    }
  }

  override fun transmitToSam(apdu: ByteArray): ByteArray? {
    return try {
      val response = samReader.transmit(apdu)
      response
    } catch (e: NullPointerException){
      null
    }
  }

  override fun transmitToCard(apdu: ByteArray): ByteArray? {
    for (i in 0..9) {
      try {
        val response = isoDep?.transceive(apdu)
        if (response != null) {
          return response
        }
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }
    return null
  }

  fun close(): Boolean {
    return try {
      samReader.close()
      true
    } catch (e: TelpoException) {
      e.printStackTrace()
      false
    }
  }
}
