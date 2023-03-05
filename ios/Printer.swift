import Foundation

@objc(Printer)
class Printer: NSObject {
  @objc(print:withResolver:withRejecter:)
  func printHTML(printActions: NSArray, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
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
    
  @objc(printHtml:withResolver:withRejecter:)
  func printHTML(html: String, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    resolve(true)
  }
}
