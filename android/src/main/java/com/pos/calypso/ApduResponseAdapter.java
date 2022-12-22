package com.pos.calypso;

import org.eclipse.keyple.core.util.json.JsonUtil;

import java.util.Arrays;

public final class ApduResponseAdapter implements ApduResponseApi {

  private final byte[] apdu;
  private final int statusWord;

  /**
   * (package-private)<br>
   * Builds an APDU response from an array of bytes from the card, computes the status word.
   *
   * @param apdu A array of at least 2 bytes.
   * @since 2.0.0
   */
  public ApduResponseAdapter(byte[] apdu) {
    this.apdu = apdu;
    statusWord = ((apdu[apdu.length - 2] & 0x000000FF) << 8) + (apdu[apdu.length - 1] & 0x000000FF);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public byte[] getApdu() {
    return this.apdu;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public byte[] getDataOut() {
    return Arrays.copyOfRange(this.apdu, 0, this.apdu.length - 2);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public int getStatusWord() {
    return statusWord;
  }

  /**
   * Converts the APDU response into a string where the data is encoded in a json format.
   *
   * @return A not empty String
   * @since 2.0.0
   */
  @Override
  public String toString() {
    return "APDU_RESPONSE = " + JsonUtil.toJson(this);
  }
}
