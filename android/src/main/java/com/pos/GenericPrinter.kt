package com.pos

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext

class GenericPrinter(private val reactApplicationContext: ReactApplicationContext): Printer() {
  override fun print(actions: List<PrintAction>, promise: Promise) {
    promise.resolve(true)
  }

  override fun open(promise: Promise) {
    promise.resolve(true)
  }

  override fun close(promise: Promise) {
    promise.resolve(true)
  }

  companion object {
    const val TAG = "GenericPrinter"
  }
}
