package com.pos

import android.os.Build
import com.facebook.react.bridge.*


class PrinterModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private val isFamoco = Build.MANUFACTURER.equals("wizarPOS")
  private val isTelpo = Build.MODEL.startsWith("TPS")
  private val isArm = Build.CPU_ABI.lowercase() in listOf("armeabi-v7a", "arm64-v8a", "armeabi")

  private val printer: Printer by lazy {
    if (isArm) {
      if (isFamoco) {
        FamocoPrinter(reactApplicationContext)
      } else if (isTelpo) {
        TelpoPrinter(reactApplicationContext)
      } else {
        GenericPrinter(reactApplicationContext)
      }
    } else {
      GenericPrinter(reactApplicationContext)
    }
  }

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun print(printActions: ReadableArray, promise: Promise) {
    val actions = getPrintActions(printActions)
    printer.print(actions, promise)
  }

  @ReactMethod
  fun open(promise: Promise) {
    printer.open(promise)
  }

  @ReactMethod
  fun close(promise: Promise) {
    printer.close(promise)
  }

  @ReactMethod
  fun printHtml(printActions: ReadableArray, promise: Promise) {
    val actions = getPrintActions(printActions)
    var html = ""
    actions.forEach {
      html += printer.printHtml(it)
    }
    promise.resolve(html)
  }

  private fun getPrintActions(printActions: ReadableArray): List<Printer.PrintAction> {
    val actions = mutableListOf<Printer.PrintAction>()
    printActions.toArrayList().forEach {
      it as HashMap<*, *>
      val type = it["type"] as String
      when (type) {
        "logo" -> {
          val data = it["data"] as String
          val options = Printer.PrintAction.LogoOptions(it["options"] as HashMap<*, *>?)
          actions.add(Printer.PrintAction.Logo(data, options))
        }
        "text" -> {
          val data = it["data"] as String
          val options = Printer.PrintAction.TextOptions(it["options"] as HashMap<*, *>)
          actions.add(Printer.PrintAction.Text(data, options))
        }
        "newLine" -> {
          val data = it["data"] as Double
          actions.add(Printer.PrintAction.NewLine(data.toInt()))
        }
      }
    }
    return actions;
  }

  companion object {
    const val NAME = "Printer"
  }
}
