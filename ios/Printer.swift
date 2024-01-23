import Foundation

@objc(Printer)
class Printer: NSObject {
  @objc(print:withResolver:withRejecter:)
  func print(printActions: NSArray, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    resolve(true)
  }
    
  @objc(open:withRejecter:)
  func open(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    resolve(true)
  }

  @objc(close:withRejecter:)
  func close(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    resolve(true)
  }
    
  // TODO: review this code, ChatGPT wrote it
  @objc(printHtml:withResolver:withRejecter:)
  func printHTML(printActions: NSArray, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) -> Void {
    var html = ""

    for action in printActions {
      if let actionDict = action as? [AnyHashable: Any], let type = actionDict["type"] as? String {
        switch type {
        case "logo":
          if let data = actionDict["data"] as? String {
            let logoOptions = PrintAction.LogoOptions(options: actionDict["options"] as? [AnyHashable: Any])
            let printAction = PrintAction.Logo(data: data, options: logoOptions)
            
            let align: String
            switch printAction.options?.align {
              case .center:
                align = "center"
              case .right:
                align = "right"
              default:
                align = "center"
            }

            let data = printAction.data

            html += "<div style=\"text-align: \(align)\"><img src=\"data:image/png;base64, \(data)\"/></div>"
          }
        case "newLine":
          if let data = actionDict["data"] as? Int {
            let printAction = PrintAction.NewLine(data: data)
            
            let data = printAction.data
            html += "<div style=\"height: \(data * 10)px;\"></div>"

          }
        case "text":
          if let data = actionDict["data"] as? String, let optionsDict = actionDict["options"] as? [AnyHashable: Any] {
            let textOptions = PrintAction.TextOptions(options: optionsDict)
            let printAction = PrintAction.Text(data: data, options: textOptions)
            
            let align: String
            switch printAction.options.align {
              case .center:
                align = "center"
              case .right:
                align = "right"
              default:
                align = "center"
            }

            let size = printAction.options.size
            let fontWeight: String
            switch printAction.options.fontWeight {
              case .bold:
                fontWeight = "bold"
              default:
                fontWeight = "normal"
            }

            let data = printAction.data
            
            
            html += "<div style=\"text-align: \(align)\"><span style=\"font-size: \(size)px; font-weight: \(fontWeight)\">\(data)</span></div>"
          }
        default:
          break
        }
      }
    }

    resolve(html)
  }
}
