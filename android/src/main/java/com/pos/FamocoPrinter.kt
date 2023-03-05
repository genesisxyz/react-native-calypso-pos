package com.pos

import android.graphics.Bitmap
import android.os.Handler
import com.cloudpos.DeviceException
import com.cloudpos.POSTerminal
import com.cloudpos.printer.PrinterDevice
import com.cloudpos.sdk.printer.html.PrinterHtmlListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext

class FamocoPrinter(private val reactApplicationContext: ReactApplicationContext): Printer() {
  private var printerDevice = POSTerminal.getInstance(reactApplicationContext).getDevice(POSTerminal.DEVICE_NAME_PRINTER) as PrinterDevice

  override fun print(actions: List<PrintAction>, promise: Promise) {
    var html = ""
    actions.forEach {
      html += super.printHtml(it)
    }

    Handler(reactApplicationContext.mainLooper).post {
      val sbCommand = StringBuilder()
      sbCommand.append(html)
      try {
        printerDevice.printHTML(reactApplicationContext, sbCommand.toString(), object: PrinterHtmlListener {
          override fun onGet(p0: Bitmap?, p1: Int) {

          }
          override fun onFinishPrinting(p0: Int) {
            promise.resolve(true)
          }
        })
      } catch (e: Exception) {
        e.printStackTrace()
        promise.resolve(false)
      }
    }
  }

  override fun open(promise: Promise) {
    try {
      printerDevice.open()
      promise.resolve(true)
    } catch (e: DeviceException) {
      e.printStackTrace()
      promise.resolve(e.code == -1)
    }
  }

  override fun close(promise: Promise) {
    try {
      printerDevice.close()
      promise.resolve(true)
    } catch (e: DeviceException) {
      promise.resolve(false)
    }
  }
}
