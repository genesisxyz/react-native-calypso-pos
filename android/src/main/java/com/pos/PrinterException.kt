package com.pos

class PrinterException(val code: String, override val message: String) : Exception(message) {

  override fun toString(): String {
    return "Code:" + this.code + " " + super.toString()
  }

  companion object {
    val NO_PAPER = "NO_PAPER"
    val OVERHEAT = "OVERHEAT"
    val OVERFLOW = "OVERFLOW"
    val UNKNOWN = "UNKNOWN"
  }
}
