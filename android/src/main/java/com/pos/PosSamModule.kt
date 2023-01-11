package com.pos

import android.os.Build
import android.widget.Toast
import com.facebook.react.bridge.*
import com.pos.byte_stuff.ByteConvertReactNativeUtil
import com.pos.byte_stuff.ByteUtils
import com.pos.calypso.Calypso


class PosSamModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  private lateinit var device: Card

  private val isFamoco = Build.MANUFACTURER.equals("wizarPOS")

  override fun getName(): String {
    return NAME
  }

  override fun getConstants(): MutableMap<String, Any> =
    hashMapOf(
      // Calypso
      "ZERO_TIME_MILLIS" to Calypso.ZERO_TIME_MILLIS,
      "SAM_CHALLENGE_LENGTH_BYTES" to Calypso.SAM_CHALLENGE_LENGTH_BYTES,
      "AID" to ByteConvertReactNativeUtil.byteArrayToReadableArray(Calypso.AID),
      "EF_ENVIRONMENT_CARD_STATUS_INDEX" to Calypso.EF_ENVIRONMENT_CARD_STATUS_INDEX,
      "EF_ENVIRONMENT_CARD_STATUS_MASK" to Calypso.EF_ENVIRONMENT_CARD_STATUS_MASK,
      "EF_ENVIRONMENT_EXPIRATION_MASK" to ByteConvertReactNativeUtil.byteArrayToReadableArray(Calypso.EF_ENVIRONMENT_EXPIRATION_MASK),
      "EF_ENVIRONMENT_EXPIRATION_INDEX" to Calypso.EF_ENVIRONMENT_EXPIRATION_INDEX,
      "EF_ENVIRONMENT_EXPIRATION_LENGTH" to Calypso.EF_ENVIRONMENT_EXPIRATION_LENGTH,
      "EF_ENVIRONMENT_ISSUE_DATE_INDEX" to Calypso.EF_ENVIRONMENT_ISSUE_DATE_INDEX,
      "EF_ENVIRONMENT_ISSUE_DATE_LENGTH" to Calypso.EF_ENVIRONMENT_ISSUE_DATE_LENGTH,
      "EF_ENVIRONMENT_DATA_FORMAT_INDEX" to Calypso.EF_ENVIRONMENT_DATA_FORMAT_INDEX,
      "EF_ENVIRONMENT_CARD_CIRCUIT_INDEX" to Calypso.EF_ENVIRONMENT_CARD_CIRCUIT_INDEX,
      "CARD_DATA_FORMAT" to Calypso.CARD_DATA_FORMAT,
      "CARD_BIP_CIRCUIT" to Calypso.CARD_BIP_CIRCUIT,
      "EF_ENVIRONMENT_TAX_CODE_INDEX" to Calypso.EF_ENVIRONMENT_TAX_CODE_INDEX,
      "EF_ENVIRONMENT_TAX_CODE_LENGTH" to Calypso.EF_ENVIRONMENT_TAX_CODE_LENGTH,
    )

  @ReactMethod
  fun init(promise: Promise) {
    device = if (isFamoco) {
      Famoco(reactApplicationContext)
    } else {
      Telpo(reactApplicationContext)
    }
    device.init(promise)
  }

  @ReactMethod
  fun writeToCard(apdu: ReadableArray, promise: Promise) {
    device.writeToCard(apdu, promise)
  }

  @ReactMethod
  fun readRecordsFromCard(promise: Promise) {
    device.readRecordsFromCard(promise)
  }

  companion object {
    const val NAME = "PosSam"
  }
}
