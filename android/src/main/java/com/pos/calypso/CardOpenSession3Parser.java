package com.pos.calypso;

import java.util.Arrays;

public final class CardOpenSession3Parser extends AbstractCardOpenSessionParser {

  private boolean isExtendedModeSupported;

  /**
   * Instantiates a new CardOpenSession3Parser from the response.
   *
   * @param response from CardOpenSession3Parser.
   * @param builder the reference to the builder that created this parser.
   * @since 2.0.0
   */
  public CardOpenSession3Parser(ApduResponseApi response, CardOpenSession3Builder builder) {
    super(response, builder);
  }

  @Override
  SecureSession toSecureSession(byte[] apduResponseData) {
    boolean previousSessionRatified;
    boolean manageSecureSessionAuthorized;
    int offset;

    isExtendedModeSupported = ((CardOpenSession3Builder) builder).isIsExtendedModeSupported();

    if (!isExtendedModeSupported) {
      offset = 0;
      previousSessionRatified = (apduResponseData[4] == (byte) 0x00);
      manageSecureSessionAuthorized = false;
    } else {
      offset = 4;
      previousSessionRatified = (apduResponseData[8] & 0x01) == (byte) 0x00;
      manageSecureSessionAuthorized = (apduResponseData[8] & 0x02) == (byte) 0x02;
    }

    byte kif = apduResponseData[5 + offset];
    byte kvc = apduResponseData[6 + offset];
    int dataLength = apduResponseData[7 + offset];
    byte[] data = Arrays.copyOfRange(apduResponseData, 8 + offset, 8 + offset + dataLength);

    return new SecureSession(
        Arrays.copyOfRange(apduResponseData, 0, 3),
        Arrays.copyOfRange(apduResponseData, 3, 4 + offset),
        previousSessionRatified,
        manageSecureSessionAuthorized,
        kif,
        kvc,
        data,
        apduResponseData);
  }
}
