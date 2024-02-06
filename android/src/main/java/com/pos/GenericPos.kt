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
import com.pos.byteUtils.ByteConvertStringUtil
import kotlinx.coroutines.*
import java.io.IOException

open class GenericPos(private val reactContext: ReactApplicationContext): CardManager(), LifecycleEventListener {
  protected var isoDep: IsoDep? = null

  protected var nfcAdapter: NfcAdapter? = null

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
          job?.cancel("error", PosException(PosException.CARD_NOT_PRESENT, "Card not present"))
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

    val pendingIntent = PendingIntent.getActivity(
      activity,
      0,
      Intent(activity, activity!!.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
      else
        PendingIntent.FLAG_UPDATE_CURRENT
    )
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

  protected var posIsInitialized = false

  override suspend fun init(promise: Promise) {
    if (posIsInitialized) {
      promise.resolve(true)
      return
    }

    samId = ""
    posIsInitialized = true
    promise.resolve(true)
  }

  protected var job: Job? = null

  override fun close() {
    try {
      job?.cancel("close", PosException(PosException.PENDING_REQUEST, "Pending request"))
      disconnectCard()
      disconnectSam()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun transmitToSam(apdu: ByteArray): ByteArray? {
    return null
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
    samIsConnected = false
  }

  override fun disconnectSam() {
    samIsConnected = false
  }

  override fun connectCard() {
    if (cardIsConnected) return
    cardIsConnected = try {
      isoDep?.connect()
      true
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw PosException(PosException.CARD_CONNECT_FAIL, "Card connect fail")
      }
      true
    }
  }

  override fun disconnectCard() {
    if (!cardIsConnected) return
    cardIsConnected = try {
      isoDep?.run {
        // TODO: crash on Android 14, can we use the tag outside the intent?
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          NfcAdapter.getDefaultAdapter(reactContext).ignore(this.tag, 1000, null, null)
        }
        */
        try {
          this.close()
        } catch (e: SecurityException) {
          e.printStackTrace()
        }
      }
      isoDep = null
      false
    } catch (e: DeviceException) {
      e.printStackTrace()
      if (e.code == -1) {
        throw PosException(PosException.CARD_DISCONNECT_FAIL, "Card disconnect fail")
      }
      false
    }
  }

  override suspend fun waitForCard() {

  }

  override suspend fun readRecordsFromCard(options: ReadableArray, promise: Promise) {
    job = GlobalScope.launch(start = CoroutineStart.LAZY) {
      super.readRecordsFromCard(options, promise)
    }
  }

  override suspend fun readCardId(promise: Promise) {
    job = GlobalScope.launch(start = CoroutineStart.LAZY) {
      super.readCardId(promise)
    }
  }
}
