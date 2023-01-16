package com.pos

class PosException(val code: Int, override val message: String) : Exception(message) {

  override fun toString(): String {
    return "Code:" + this.code + " " + super.toString()
  }

  companion object {
    val CARD_NOT_SUPPORTED = 100
    val CARD_NOT_PRESENT = 101
    val TRANSMIT_APDU_COMMAND = 102
    val PENDING_REQUEST = 103
    val SAM_CONNECT_FAIL = 104
  }
}
