package com.pos

import android.graphics.BitmapFactory
import android.util.Base64
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.telpo.tps550.api.TelpoException
import com.telpo.tps550.api.printer.UsbThermalPrinter

class TelpoPrinter(reactApplicationContext: ReactApplicationContext): Printer() {
  private val printer = UsbThermalPrinter(reactApplicationContext)

  override fun print(actions: List<PrintAction>, promise: Promise) {
    actions.forEach {
      try {
        printAction(it)
      } catch (e: Exception) {
        if (e is TelpoException) {
          val status = printer.checkStatus()
          when (status) {
            UsbThermalPrinter.STATUS_NO_PAPER, 16 -> promise.reject(PrinterException.NO_PAPER, "No paper")
            UsbThermalPrinter.STATUS_OVER_HEAT -> promise.reject(PrinterException(PrinterException.OVERHEAT, "Overheating"))
            UsbThermalPrinter.STATUS_OVER_FLOW -> promise.reject(PrinterException(PrinterException.OVERFLOW, "Overflow"))
            else -> promise.reject(PrinterException(PrinterException.UNKNOWN, "Unknown"))
          }

          return;
        }
        promise.resolve(false)
        return
      }
    }
    promise.resolve(true)
  }

  override fun open(promise: Promise) {
    try {
      printer.start(0)
      printer.setGray(6)
      promise.resolve(true)
    } catch (e: TelpoException) {
      promise.resolve(false)
    }
  }

  override fun close(promise: Promise) {
    printer.stop()
    promise.resolve(true)
  }

  private fun printAction(printAction: PrintAction) {
    when (printAction) {
      is PrintAction.Logo -> {
        printAction.options?.run {
          setAlign(this.align)
        }
        printLogo(printAction.data)
      }
      is PrintAction.Text -> {
        printAction.options.run {
          setAlign(this.align)
          setFontWeight(this.fontWeight)
          setTextSize(this.size.toDouble())
        }
        addString(printAction.data)
        printString()
      }
      is PrintAction.NewLine -> {
        walkPaper(printAction.data.toDouble())
      }
    }
  }

  private fun setFontWeight(fontWeight: PrintAction.FontWeight) {
    printer.setBold(fontWeight == PrintAction.FontWeight.BOLD)
  }

  private fun setAlign(align: PrintAction.Align) {
    val mode = when (align) {
      PrintAction.Align.LEFT -> {
        0;
      }
      PrintAction.Align.CENTER -> {
        1;
      }
      PrintAction.Align.RIGHT -> {
        2;
      }
    }
    printer.setAlgin(mode)
  }

  private fun setTextSize(size: Double) {
    printer.setTextSize(size.toInt())
  }

  private fun addString(content: String) {
    printer.addString(content)
  }

  private fun printString() {
    printer.printString()
  }

  private fun walkPaper(line: Double) {
    printer.walkPaper(line.toInt())
  }

  private fun printLogo(image: String) {
    val imageBytes = Base64.decode(image, 0)
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    printer.printLogo(bitmap, false)
  }

  companion object {
    const val TAG = "TelpoPrinter"
  }
}
