//
//  PrintAction.swift
//  react-native-calypso-pos
//
//  Created by Damiano Collu on 23/01/24.
//

import Foundation

// TODO: review this code, ChatGPT wrote it
class PrintAction {
  class Logo: PrintAction {
    let data: String
    let options: LogoOptions?
    
    init(data: String, options: LogoOptions? = nil) {
      self.data = data
      self.options = options
      super.init()
    }
  }
  
  class NewLine: PrintAction {
    let data: Int
    
    init(data: Int) {
      self.data = data
      super.init()
    }
  }
  
  class Text: PrintAction {
    let data: String
    let options: TextOptions
    
    init(data: String, options: TextOptions) {
      self.data = data
      self.options = options
      super.init()
    }
  }

  class LogoOptions {
    var align: Align
    
    init(align: Align = .left) {
      self.align = align
    }
    
    convenience init(options: [AnyHashable: Any]?) {
      self.init(align: .left)
      if let alignString = options?["align"] as? String {
        switch alignString {
        case "left":
          align = .left
        case "center":
          align = .center
        case "right":
          align = .right
        default:
          break
        }
      }
    }
  }

  class TextOptions {
    var size: Int
    var fontWeight: FontWeight
    var align: Align
    
    init(size: Int, fontWeight: FontWeight = .normal, align: Align = .left) {
      self.size = size
      self.fontWeight = fontWeight
      self.align = align
    }
    
    convenience init(options: [AnyHashable: Any]) {
      self.init(size: 12, fontWeight: .normal, align: .left)
      if let sizeDouble = options["size"] as? Double {
        self.size = Int(sizeDouble)
      }
      if let fontWeightString = options["fontWeight"] as? String {
        self.fontWeight = FontWeight(rawValue: fontWeightString) ?? .normal
      }
      if let alignString = options["align"] as? String {
        self.align = Align(rawValue: alignString) ?? .left
      }
    }
  }

  enum FontWeight: String {
    case normal, bold
  }

  enum Align: String {
    case left, center, right
  }
}
