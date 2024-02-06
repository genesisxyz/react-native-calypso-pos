package com.pos

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext

class GenericPrinter(private val reactApplicationContext: ReactApplicationContext): Printer() {

  override fun print(actions: List<PrintAction>, promise: Promise) {
    promise.reject(PrinterException(PrinterException.UNKNOWN, "TODO: implementation missing"))
  }

  override fun open(promise: Promise) {
    promise.resolve(false)
  }

  override fun close(promise: Promise) {
    promise.resolve(false)
  }

  companion object {
    const val TAG = "GenericPrinter"
  }
}
