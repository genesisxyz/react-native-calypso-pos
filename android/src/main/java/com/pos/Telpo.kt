package com.pos

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.cloudpos.DeviceException
import com.facebook.react.bridge.*
import com.pos.byteUtils.ByteConvertReactNativeUtil
import com.pos.calypso.*
import com.telpo.tps550.api.reader.SmartCardReader
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

class Telpo(private val reactContext: ReactApplicationContext) : CardManager(), LifecycleEventListener {

  private lateinit var samReader: SmartCardReader
  private lateinit var samId: String
  private lateinit var isoDep: IsoDep

  private var nfcAdapter: NfcAdapter? = null

  private val baseActivityEventListener = object : BaseActivityEventListener() {
    override fun onNewIntent(intent: Intent?) {
      super.onNewIntent(intent)
      if (NfcAdapter.ACTION_TECH_DISCOVERED == intent?.action) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        isoDep = IsoDep.get(tag)

        if (job != null) {
          job?.start()
          job = null
        }
      }
    }
  }

  // region LifecycleEventListener

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

  }

  // endregion LifecycleEventListener

  init {
    reactContext.addActivityEventListener(baseActivityEventListener)
    reactContext.addLifecycleEventListener(this)
  }

  override suspend fun init(promise: Promise) {
    // TODO: check SAM slot
    samReader = SmartCardReader(reactContext, SmartCardReader.SLOT_PSAM1)

    try {
      connectSam()
      promise.resolve(true)
    } catch (e: Throwable) {
      promise.resolve(false)
    } finally {
      disconnectSam()
    }
  }

  private var job: Job? = null

  override suspend fun readRecordsFromCard(promise: Promise) {
    job = GlobalScope.launch(start = CoroutineStart.LAZY) {

      try {
        connectCard()

        val selectApplicationBuilder = SelectApplicationBuilder(
          SelectApplicationBuilder.SELECT_FIRST_OCCURRENCE_RETURN_FCI)

        val selectApplicationResponseAdapter = ApduResponseAdapter(
          transmitToCard(selectApplicationBuilder.apdu))

        val selectApplicationParser = selectApplicationBuilder
          .createResponseParser(selectApplicationResponseAdapter)

        selectApplicationParser.checkStatus()

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
          CardReadRecordsBuilder.ReadMode.ONE_RECORD, 0
        )

        val readRecordsResponseAdapter = ApduResponseAdapter(transmitToCard(readRecordsBuilder.apdu))

        val readRecordsParser = readRecordsBuilder.createResponseParser(readRecordsResponseAdapter)

        readRecordsParser.checkStatus()

        val records: Map<Int, ByteArray> = readRecordsParser.records

        val readableMap = Arguments.createMap()
        for ((key, record) in records) {
          val array = ByteConvertReactNativeUtil.byteArrayToReadableArray(record)
          readableMap.putArray(key.toString(), array)
        }

        disconnectCard()

        promise.resolve(readableMap)
      } catch (e: Exception) {
        promise.reject(e)
      }
    }
  }

  override suspend fun writeToCard(apdu: ReadableArray, promise: Promise) {
    job = GlobalScope.launch(start = CoroutineStart.LAZY) {
      super.writeToCard(apdu, promise)
    }
  }

  override fun connectCard() {
    try {
      isoDep.connect()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
      }
    }
  }

  override fun disconnectCard() {
    try {
      isoDep.close()
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code != -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
      }
    }
  }

  override suspend fun waitForCard() {

  }

  override fun transmitToSam(apdu: ByteArray): ByteArray? {
    return try {
      val response = samReader.transmit(apdu)
      response
    } catch (e: NullPointerException) {
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

  override fun connectSam() {
    // TODO: manage return of open and iccPowerOn
    val isOpen = samReader.open()
    val isIccPowerOn = samReader.iccPowerOn()

    if (!isOpen || !isIccPowerOn) {
      throw PosException(PosException.SAM_CONNECT_FAIL, "Cannot connect to SAM")
    }
  }

  override fun disconnectSam() {
    samReader.close()
  }
}
