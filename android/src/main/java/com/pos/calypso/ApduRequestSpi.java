package com.pos.calypso;

import java.util.Set;

public interface ApduRequestSpi {

  /**
   * Gets the APDU bytes to be sent to the card.
   *
   * @return A array of at least 4 bytes.
   * @since 1.0.0
   */
  byte[] getApdu();

  /**
   * Gets the list of status words that must be considered successful for the APDU.
   *
   * @return A set of integer values containing at least 9000h.
   * @since 1.0.0
   */
  Set<Integer> getSuccessfulStatusWords();

  /**
   * Gets the information about this APDU request (e.g. command name).
   *
   * <p>These information are intended to improve the logging.
   *
   * @return Null if no information has been defined.
   * @since 1.0.0
   */
  String getInfo();
}
