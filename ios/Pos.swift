import Foundation
import CoreNFC

@objc(Pos)
class Pos: NSObject, NFCNDEFReaderSessionDelegate {
  var session: NFCNDEFReaderSession?
    
  override init() {
    super.init()
    session = NFCNDEFReaderSession(delegate: self, queue: DispatchQueue.main, invalidateAfterFirstRead: false)
    session?.begin()
    if NFCNDEFReaderSession.readingAvailable {
      print("NFC is avaiable")
    }
    else {
      print("NFC is NOT avaiable")
    }
  }
  
  @objc(init:withRejecter:)
  func `init`(resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    resolve(true)
  }
  
  @objc(close)
  func close() -> Void {
    
  }
  
  @objc(readCardId:withRejecter:)
  func readCardId(resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
    // resolve(["samId": "00 00 00", "cardId": "123456789",])
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
  }
  
  @objc(writeToCardUpdate:withOptions:withResolver:withRejecter:)
  func writeToCardUpdate(apdu: NSArray, options: NSDictionary, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
      resolve(nil)
  }
  
  // MARK: NFCNDEFReaderSessionDelegate
  
  func readerSession(_ session: NFCNDEFReaderSession, didInvalidateWithError error: Error) {
    
  }
  
  func readerSession(_ session: NFCNDEFReaderSession, didDetectNDEFs messages: [NFCNDEFMessage]) {
    for message in messages {
      for record in message.records {
        if let string = String(data: record.payload, encoding: .ascii) {
          print(string)
        }
      }
    }
  }
}
