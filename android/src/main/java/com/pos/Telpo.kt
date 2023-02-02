package com.pos

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import com.cloudpos.DeviceException
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.pos.byteUtils.ByteConvertStringUtil
import com.pos.calypso.CardReadRecordsBuilder
import com.telpo.tps550.api.reader.SmartCardReader
import kotlinx.coroutines.*
import java.io.IOException

class Telpo(private val reactContext: ReactApplicationContext) : CardManager(), LifecycleEventListener {

  private lateinit var samReader: SmartCardReader
  private lateinit var samId: String
  private var isoDep: IsoDep? = null

  private var nfcAdapter: NfcAdapter? = null

  private val baseActivityEventListener = object : BaseActivityEventListener() {
    override fun onNewIntent(intent: Intent?) {
      super.onNewIntent(intent)
      if (NfcAdapter.ACTION_TECH_DISCOVERED == intent?.action) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null && (isoDep == null || !isoDep!!.isConnected)) {
          isoDep = IsoDep.get(tag)
          isoDep!!.timeout = 5000 // 5 seconds

          val cardIdHex = ByteConvertStringUtil.bytesToHexString(tag.id)
          cardId = cardIdHex.replace("\\s+".toRegex(), "").toLong(16).toString()

          if (job != null) {
            val params = Arguments.createMap().apply {
              putString("status", "detected")
            }
            sendEvent(reactContext, "CardStatus", params)
            job?.start()
            job = null
          }
        } else {
          // TODO: to be tested
          job?.cancel("error", PosException(PosException.CARD_NOT_PRESENT, "Can't read card tag"))
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

  private var posIsInitialized = false

  override suspend fun init(promise: Promise) {
    if (posIsInitialized) {
      promise.resolve(true)
      return
    }

    // TODO: check SAM slot
    samReader = SmartCardReader(reactContext, SmartCardReader.SLOT_PSAM1)

    try {
      connectSam()
      posIsInitialized = true
      promise.resolve(true)
      disconnectSam()
    } catch (e: Throwable) {
      promise.resolve(false)
    }
  }

  private var job: Job? = null

  override suspend fun readRecordsFromCard(options: ReadableMap, promise: Promise) {
    job = GlobalScope.launch(start = CoroutineStart.LAZY) {
      super.readRecordsFromCard(options, promise)
    }
  }

  override suspend fun writeToCardUpdate(apdu: ReadableArray, options: ReadableMap, promise: Promise) {
    job = GlobalScope.launch(start = CoroutineStart.LAZY) {
      super.writeToCardUpdate(apdu, options, promise)
    }
  }

  override fun connectCard() {
    if (cardIsConnected) return
    cardIsConnected = try {
      isoDep?.connect()
      true
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
      }
      true
    }
  }

  override fun disconnectCard() {
    if (!cardIsConnected) return
    cardIsConnected = try {
      isoDep?.run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          NfcAdapter.getDefaultAdapter(reactContext).ignore(this.tag, 1000, null, null)
        }
        this.close()
      }
      isoDep = null
      false
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw PosException(PosException.CARD_NOT_PRESENT, "Card not present")
      }
      false
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
    if (isoDep?.isConnected == true) {
      try {
        val received = isoDep?.transceive(apdu)
        if (received != null)
          return received
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }
    return null
  }

  override fun connectSam() {
    if (samIsConnected) return
    // TODO: manage return of open and iccPowerOn
    val isOpen = samReader.open()
    val isIccPowerOn = samReader.iccPowerOn()

    if (!isOpen || !isIccPowerOn) {
      throw PosException(PosException.SAM_CONNECT_FAIL, "Cannot connect to SAM")
    }
    samIsConnected = true
  }

  override fun disconnectSam() {
    if (!samIsConnected) return
    samReader.iccPowerOff()
    samReader.close()
    samIsConnected = false
  }

  override fun close() {
    try {
      job?.cancel("message", PosException(PosException.PENDING_REQUEST, "Close called"))
      disconnectCard()
      disconnectSam()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
