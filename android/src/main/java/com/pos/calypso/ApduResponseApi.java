package com.pos.calypso;

import java.io.Serializable;

public interface ApduResponseApi extends Serializable {

  /**
   * Gets the raw data received from the card (including the status word).
   *
   * @return An array of at least 2 bytes.
   * @since 1.0.0
   */
  byte[] getApdu();

  /**
   * Gets the data part of the response received from the card (excluding the status word).
   *
   * @return A not null byte array.
   * @since 1.0.0
   */
  byte[] getDataOut();

  /**
   * Gets the status word of the APDU as an int.
   *
   * @return An integer between 0000h and FFFFh.
   * @since 1.0.0
   */
  int getStatusWord();
}
