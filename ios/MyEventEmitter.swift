import Foundation
import React

@objc(MyEventEmitter)
class MyEventEmitter: RCTEventEmitter {

    public static var shared: MyEventEmitter?

    override init() {
        super.init()
        MyEventEmitter.shared = self
    }
    
    @objc override static func requiresMainQueueSetup() -> Bool {
        return false
    }

    override func supportedEvents() -> [String]! {
        return [
            "CardStatus",
        ]
    }
    
    func cardStatus(status: String) {
        self.sendEvent(withName: "CardStatus", body: [
          "status": status
        ])
    }
}
