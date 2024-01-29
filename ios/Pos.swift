import Foundation
import CoreNFC

@objc(Pos)
class Pos: NSObject, NFCTagReaderSessionDelegate {
  var session: NFCTagReaderSession?
  
  var resolve: RCTPromiseResolveBlock?
  var reject: RCTPromiseRejectBlock?

  @objc(init:withRejecter:)
  func `init`(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    resolve(true)
  }
  
  @objc(close)
  func close() -> Void {
    resolve = nil
    reject = nil
    session?.invalidate()
    session = nil
  }
  
  @objc(readCardId:withRejecter:)
  func readCardId(resolve:@escaping RCTPromiseResolveBlock, reject:@escaping RCTPromiseRejectBlock) -> Void {
    if NFCTagReaderSession.readingAvailable {
      self.resolve = resolve
      self.reject = reject
      session = NFCTagReaderSession(pollingOption: NFCTagReaderSession.PollingOption.iso14443, delegate: self)
      session?.begin()
    } else {
      reject("UNKNOWN", "NFC not available", nil)
    }
  }
  
  @objc(readRecordsFromCard:withResolver:withRejecter:)
  func readRecordsFromCard(options: NSDictionary, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    /*
    resolve([
      "records": [
        "1": [0x05, 0x35, 0x00, 0x04, 0x93, 0xE2, 0x00, 0xA0, 0x02, 0x8B, 0xE8, 0x22, 0x54, 0x53, 0x54, 0x54, 0x53, 0x54, 0x35, 0x36, 0x44, 0x34, 0x36, 0x4C, 0x32, 0x31, 0x39, 0x47, 0xC0],
      ],
      "cardId": "123456789",
    ])
    */
    reject("UNKNOWN", "TODO: implementation missing", nil)
  }
  
  @objc(writeToCardUpdate:withOptions:withResolver:withRejecter:)
  func writeToCardUpdate(apdu: NSArray, options: NSDictionary, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
      resolve(nil)
  }
  
  // MARK: NFCTagReaderSessionDelegate
  
  func tagReaderSessionDidBecomeActive(_ session: NFCTagReaderSession) {

  }
  
  func tagReaderSession(_ session: NFCTagReaderSession, didInvalidateWithError error: Error) {
    reject?("UNKNOWN", "Session closed", nil)
    resolve = nil
    reject = nil
    self.session = nil
  }
  
  func tagReaderSession(_ session: NFCTagReaderSession, didDetect tags: [NFCTag]) {
    if case let NFCTag.iso7816(tag) = tags.first! {
      MyEventEmitter.shared?.cardStatus(status: "detected")
      
      let tagUIDData = tag.identifier
      var byteData: [UInt8] = []
      tagUIDData.withUnsafeBytes { byteData.append(contentsOf: $0) }
      var n = ""
      for byte in byteData {
        let decimalNumber = String(byte & 0xFF, radix: 16)
        n.append(decimalNumber)
      }
      
      resolve?(
        [
          "samId": nil,
          "cardId": UInt64(n, radix: 16),
        ]
      )
      resolve = nil
      reject = nil
      session.invalidate()
    }
    else if case let NFCTag.miFare(tag) = tags.first! {
      MyEventEmitter.shared?.cardStatus(status: "detected")
      
      let tagUIDData = tag.identifier
      var byteData: [UInt8] = []
      tagUIDData.withUnsafeBytes { byteData.append(contentsOf: $0) }
      var n = ""
      for byte in byteData {
        let decimalNumber = String(byte & 0xFF, radix: 16)
        n.append(decimalNumber)
      }
      
      resolve?(
        [
          "samId": nil,
          "cardId": UInt64(n, radix: 16),
        ]
      )
      resolve = nil
      reject = nil
      session.invalidate()
    }
  }
}
