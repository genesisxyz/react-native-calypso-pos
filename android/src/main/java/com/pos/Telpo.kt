package com.pos

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.cloudpos.DeviceException
import com.facebook.react.bridge.*
import com.pos.byte_stuff.ByteConvertReactNativeUtil
import com.pos.calypso.*
import com.telpo.tps550.api.TelpoException
import com.telpo.tps550.api.reader.SmartCardReader
import java.io.IOException

class Telpo(private val reactContext: ReactApplicationContext): Card(), LifecycleEventListener {

  private lateinit var samReader: SmartCardReader
  private lateinit var samId: String
  private lateinit var isoDep: IsoDep

  private var promise: Promise? = null
  private lateinit var apdu: ByteArray

  private var nfcAdapter: NfcAdapter? = null

  override fun onHostResume() {
    val activity = reactContext.currentActivity
    nfcAdapter = NfcAdapter.getDefaultAdapter(activity)

    if (nfcAdapter == null) {
      // Handle case where device does not support NFC
      return
    }

    val pendingIntent = PendingIntent.getActivity(activity, 0,
      Intent(activity, activity!!.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
    val intentFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)

    try {
      nfcAdapter?.enableForegroundDispatch(activity, pendingIntent,
        arrayOf(intentFilter), arrayOf(arrayOf(IsoDep::class.java.name)))
    } catch (e: IllegalStateException) {
      // Handle case where app is not in foreground
    }
  }

  override fun onHostPause() {
    nfcAdapter?.disableForegroundDispatch(reactContext.currentActivity)
  }

  override fun onHostDestroy() {
    TODO("Not yet implemented")
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    if (NfcAdapter.ACTION_TECH_DISCOVERED == intent?.action) {
      val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      isoDep = IsoDep.get(tag)
      try {
        isoDep.connect()
        // Send APDU commands and receive responses using isoDep methods
          try {

            val selectApplicationBuilder = SelectApplicationBuilder(
              SelectApplicationBuilder.SELECT_FIRST_OCCURRENCE_RETURN_FCI)

            if(!isoDep.isConnected){
              isoDep.connect()
            }

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

            if (!selectFileParser.isSuccess ||
              selectFileParser.proprietaryInformation == null) {
              throw PosException(PosException.CARD_NOT_SUPPORTED, "Card not supported")
            }

            val readRecordsBuilder = CardReadRecordsBuilder(
              Calypso.SFI_EF_ENVIRONMENT, 1,
              CardReadRecordsBuilder.ReadMode.ONE_RECORD, 0)

            val readRecordsResponseAdapter = ApduResponseAdapter(transmitToCard(readRecordsBuilder.apdu))

            val readRecordsParser = readRecordsBuilder.createResponseParser(readRecordsResponseAdapter)

            readRecordsParser.checkStatus()

            val records: Map<Int, ByteArray> = readRecordsParser.records

            val readableMap = Arguments.createMap()
            for ((key, record) in records) {
              val array = ByteConvertReactNativeUtil.byteArrayToReadableArray(record)
              readableMap.putArray(key.toString(), array)
            }

            promise?.resolve(readableMap)

          } catch (e: Exception) {
            e.printStackTrace()
            if (e is PosException) {
              promise?.reject(e)
            } else {
              promise?.reject(PosException(PosException.CARD_NOT_PRESENT, "Card not present"))
            }
          }
        // ...
      } catch (e: IOException) {
        // Handle exception when connecting to tag
      } finally {
        disconnectCard()
      }
    }
  }

  init {
    reactContext.addActivityEventListener(this)
    reactContext.addLifecycleEventListener(this)
  }

  override suspend fun init(promise: Promise) {
    this.promise = promise
    open()
  }

  fun open() {
    //TODO: check SAM slot
    samReader = SmartCardReader(reactContext, SmartCardReader.SLOT_PSAM1)
    var isTelpoOpened = false
    var isTelpoPoweredOn = false
    try {
      isTelpoOpened = samReader.open()
      isTelpoPoweredOn = samReader.iccPowerOn()
      if (isTelpoPoweredOn) {
        val tempSamId = samReader.atrString;

        if (tempSamId != null) {
          samId = tempSamId.substring(24, 32)
          initialized(true)
          return
        }
      } else {
        initialized(false)
      }
    } catch (e: Throwable){
      initialized(false)
    }
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
      this.apdu = (apdu.toArrayList() as ArrayList<Int>).map { it.toByte() }.toByteArray()
      this.promise = promise
      // TODO: add code for write
      this.promise?.resolve(null)
      this.promise = null
    } else {
      promise.reject("Write/read to card already in progress")
    }
  }

  override suspend fun readRecordsFromCard(promise: Promise) {
    this.promise = promise
    val activity = reactContext.currentActivity
    if (activity == null) {
      promise.reject("E_ACTIVITY_DOES_NOT_EXIST", "Activity doesn't exist")
      return
    }

    if (nfcAdapter == null) {
      promise.reject("E_NFC_NOT_SUPPORTED", "NFC is not supported on this device")
      return
    }

    if (!nfcAdapter?.isEnabled!!) {
      promise.reject("E_NFC_DISABLED", "NFC is disabled on this device")
      return
    }
  }

  private fun connectCard() {
    try {
        isoDep.connect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
      }
    }
  }

  fun disconnectCard() {
    try {
      isoDep.close()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
      }
    }
  }

  fun getChallengeApdu(): ByteArray {
    val samGetChallengeBuilder = SamGetChallengeBuilder(Calypso.SAM_CHALLENGE_LENGTH_BYTES)
    return samGetChallengeBuilder.apdu
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
    if (isoDep.isConnected) {
      for (i in 0..9) {
        try {
          val received = isoDep.transceive(apdu)
          if (received != null)
            return received
        } catch (e: IOException) {
          e.printStackTrace()
        }
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
