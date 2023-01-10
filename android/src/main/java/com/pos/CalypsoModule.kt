package com.pos

import android.widget.Toast
import com.facebook.react.bridge.*
import com.pos.calypso.Calypso.SAM_CHALLENGE_LENGTH_BYTES

class CalypsoModule (reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext){

  override fun getName(): String {
    return CalypsoModule.NAME
  }

  override fun getConstants(): Map<String, Any> {
    val constants = HashMap<String, Any>()
    constants["SAM_CHALLENGE_LENGTH_BYTES"] = SAM_CHALLENGE_LENGTH_BYTES
    return constants
  }

  companion object {
    const val NAME = "Calypso"
  }
}
