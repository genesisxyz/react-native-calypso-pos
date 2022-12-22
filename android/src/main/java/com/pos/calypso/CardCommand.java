package com.pos.calypso;

import java.io.Serializable;

interface CardCommand extends Serializable {

  /**
   * Gets command's name.
   *
   * @return A String
   * @since 2.0.0
   */
  String getName();

  /**
   * Gets Instruction Byte (INS)
   *
   * @return A byte.
   * @since 2.0.0
   */
  byte getInstructionByte();
}
