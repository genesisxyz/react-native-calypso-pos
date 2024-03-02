package com.pos

class PosException(val code: String, override val message: String) : Exception(message) {

  override fun toString(): String {
    return "Code:" + this.code + " " + super.toString()
  }

  companion object {
    val UNKNOWN = "UNKNOWN"
    val CARD_NOT_SUPPORTED = "CARD_NOT_SUPPORTED"
    val CARD_NOT_PRESENT = "CARD_NOT_PRESENT"
    val CARD_NOT_CONNECTED = "CARD_NOT_CONNECTED"
    val CARD_CONNECT_FAIL = "CARD_CONNECT_FAIL"
    val CARD_DISCONNECT_FAIL = "CARD_DISCONNECT_FAIL"
    val TRANSMIT_APDU_COMMAND = "TRANSMIT_APDU_COMMAND"
    val PENDING_REQUEST = "PENDING_REQUEST"
    val SAM_CONNECT_FAIL = "SAM_CONNECT_FAIL"
    val SAM_DISCONNECT_FAIL = "SAM_DISCONNECT_FAIL"
    val NO_SAM_AVAILABLE = "NO_SAM_AVAILABLE"
    val CANCEL = "CANCEL"
  }
}
