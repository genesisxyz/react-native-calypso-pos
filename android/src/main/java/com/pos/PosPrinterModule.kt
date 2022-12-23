package com.pos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.cloudpos.DeviceException
import com.cloudpos.POSTerminal
import com.cloudpos.POSTerminal.DEVICE_NAME_PRINTER
import com.cloudpos.printer.PrinterDevice
import com.cloudpos.sdk.printer.html.PrinterHtmlListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.telpo.tps550.api.TelpoException
import com.telpo.tps550.api.printer.UsbThermalPrinter
import java.util.*


class PosPrinterModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private val printer = UsbThermalPrinter(reactApplicationContext)
  private lateinit var printerDevice: PrinterDevice
  private val isFamoco = Build.MANUFACTURER.equals("wizarPOS")

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun checkStatus(promise: Promise) {
    try {
      val status = printer.checkStatus()
      promise.resolve(status)
    }
    catch (e: TelpoException){
      promise.resolve(UsbThermalPrinter.STATUS_UNKNOWN)
    }
  }

  @ReactMethod
  fun open(promise: Promise) {
    if(isFamoco){
      printerDevice = POSTerminal.getInstance(reactApplicationContext).getDevice(DEVICE_NAME_PRINTER) as PrinterDevice
      try {
        printerDevice.open()
        promise.resolve(true)
      }catch (e: DeviceException){
        e.printStackTrace()
        promise.resolve(e.code == -1)
      }
    }else {
      try {
        printer.start(0)
        promise.resolve(true)
      }catch (e: TelpoException){
        promise.resolve(false)
      }
    }
  }

  @ReactMethod
  fun setGrey(level: Double) {
    if (isFamoco) {
    }else{
      printer.setGray(level.toInt())
    }
  }

  @ReactMethod
  fun setLineSpace(lineSpace: Double) {
    if (isFamoco) {
    }else {
      printer.setLineSpace(lineSpace.toInt())
    }
  }

  @ReactMethod
  fun setBold(isBold: Boolean) {
    if (isFamoco) {
    }else {
      printer.setBold(isBold)
    }
  }

  @ReactMethod
  fun setAlgin(mode: Double) {
    if (isFamoco) {
    }else {
      printer.setAlgin(mode.toInt())
    }
  }

  @ReactMethod
  fun setTextSize(size: Double) {
    if (isFamoco) {
    }else {
      printer.setTextSize(size.toInt())
    }
  }

  @ReactMethod
  fun addString(content: String) {
    if (isFamoco) {
    }else {
      printer.addString(content)
    }
  }

  @ReactMethod
  fun printString() {
    if (isFamoco) {
    }else {
      printer.printString()
    }
  }

  @ReactMethod
  fun walkPaper(line: Double) {
    if (isFamoco) {
    }else {
      printer.walkPaper(line.toInt())
    }
  }

  @ReactMethod
  fun printLogo(image: String, isBuffer: Boolean) {
    if (isFamoco) {
    }else {
      val imageBytes = Base64.decode(image, 0)
      val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
      printer.printLogo(bitmap, isBuffer)
    }
  }

  @ReactMethod
  fun printHTML(html: String, promise: Promise){
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

  @ReactMethod
  fun close(promise: Promise) {
    if (isFamoco) {
      try {
        printerDevice.close()
        promise.resolve(true)
      }catch (e: DeviceException){
        promise.resolve(false)
      }
    }else {
      printer.stop()
      promise.resolve(true)
    }
  }

  companion object {
    const val NAME = "PosPrinter"
  }
}
