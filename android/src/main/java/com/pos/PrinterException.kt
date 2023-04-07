package com.pos

class PrinterException(val code: Int, override val message: String) : Exception(message) {

  override fun toString(): String {
    return "Code:" + this.code + " " + super.toString()
  }

  companion object {
    val NO_PAPER = "NO_PAPER"
    val OVER_HEAT = 101
    val OVER_FLOW = 102
    val UNKNOWN = 103
  }
}
