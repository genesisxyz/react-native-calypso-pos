package com.pos

import android.os.Bundle
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap

class GenericPrinter(private val reactApplicationContext: ReactApplicationContext): Printer() {

  override fun print(actions: List<PrintAction>, promise: Promise) {
    val userInfo = Arguments.createMap()
    userInfo.putBoolean("isPrinterError", true)
    promise.reject(PrinterException.UNKNOWN, "TODO: implementation missing", userInfo)
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
