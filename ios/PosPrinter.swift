import Foundation

@objc(PosPrinter)
class PosPrinter: NSObject {
    
  @objc(checkStatus:withRejecter:)
  func checkStatus(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    resolve(0)
  }
    
  @objc(open:withRejecter:)
  func open(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    resolve(true)
  }
  
  @objc(setGrey:)
  func setGrey(level: Int) -> Void {
    
  }
  
  @objc(setLineSpace:)
  func setLineSpace(lineSpace: Int) -> Void {
    
  }
  
  @objc(setBold:)
  func setBold(isBold: Bool) -> Void {
    
  }
  
  @objc(setAlgin:)
  func setAlgin(mode: Int) -> Void {
    
  }
  
  @objc(setTextSize:)
  func setTextSize(size: Int) -> Void {
    
  }
  
  @objc(addString:)
  func addString(content: String) -> Void {
    
  }
  
  @objc(printString)
  func printString() -> Void {
    
  }
  
  @objc(walkPaper:)
  func walkPaper(line: Int) -> Void {
    
  }
  
  @objc(printLogo:withIsBuffer:)
  func printLogo(image: String, isBuffer: Bool = false) -> Void {
    
  }

  @objc(printHTML:withResolver:withRejecter:)
  func printHTML(html: String, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    resolve(true)
  }

  @objc(close:withRejecter:)
  func close(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    resolve(true)
  }
}
