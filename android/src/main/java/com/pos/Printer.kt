package com.pos

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap

abstract class Printer {
  sealed class PrintAction {
    data class Logo(val data: String, val options: LogoOptions? = null) : PrintAction()
    data class NewLine(val data: Int) : PrintAction()
    data class Text(val data: String, val options: TextOptions) : PrintAction()

    data class LogoOptions(
      var align: Align = Align.LEFT,
    ) {
      constructor(options: HashMap<*, *>?): this(Align.LEFT) {
        val align = options?.get("align") as String?

        when (align) {
          "left" -> {
            this.align = Align.LEFT
          }
          "center" -> {
            this.align = Align.CENTER
          }
          "right" -> {
            this.align = Align.RIGHT
          }
        }
      }
    }

    data class TextOptions(
      var size: Int,
      var fontWeight: FontWeight = FontWeight.NORMAL,
      var align: Align = Align.LEFT,
    ) {
      constructor(options: HashMap<*, *>): this(12, FontWeight.NORMAL, Align.LEFT) {
        val size = options["size"] as Double
        val fontWeight = options["fontWeight"] as String?
        val align = options["align"] as String?

        this.size = size.toInt()

        when (fontWeight) {
          "normal" -> {
            this.fontWeight = FontWeight.NORMAL
          }
          "bold" -> {
            this.fontWeight = FontWeight.BOLD
          }
        }

        when (align) {
          "left" -> {
            this.align = Align.LEFT
          }
          "center" -> {
            this.align = Align.CENTER
          }
          "right" -> {
            this.align = Align.RIGHT
          }
        }
      }
    }

    enum class FontWeight {
      NORMAL,
      BOLD
    }

    enum class Align {
      LEFT, CENTER, RIGHT
    }
  }

  abstract fun print(actions: List<PrintAction>, promise: Promise)

  abstract fun open(promise: Promise)

  abstract fun close(promise: Promise)

  fun printHtml(printAction: PrintAction): String {
    var html = ""

    when (printAction) {
      is PrintAction.Logo -> {
        val align = when (printAction.options?.align) {
          PrintAction.Align.CENTER -> "center"
          PrintAction.Align.RIGHT -> "right"
          else -> "center"
        }

        val data = printAction.data

        html = "<div style=\"text-align: ${align}\"><img src=\"data:image/png;base64, ${data}\"/></div>"
      }
      is PrintAction.Text -> {
        val align = when (printAction.options.align) {
          PrintAction.Align.CENTER -> "center"
          PrintAction.Align.RIGHT -> "right"
          else -> "center"
        }

        val size = printAction.options.size
        val fontWeight = when (printAction.options.fontWeight) {
          PrintAction.FontWeight.BOLD -> "bold"
          else -> "normal"
        }

        val data = printAction.data

        html = "<div style=\"text-align: $align\"><span style=\"font-size: ${size}px; font-weight: ${fontWeight}\">$data</span></div>"
      }
      is PrintAction.NewLine -> {
        val data = printAction.data
        html += "<div style=\"height: ${data * 10}px;\"></div>"
      }
    }

    return html
  }
}
