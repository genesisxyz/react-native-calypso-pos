package com.pos.calypso;

import org.eclipse.keyple.core.util.json.JsonUtil;

import java.util.HashSet;
import java.util.Set;

final class ApduRequestAdapter implements ApduRequestSpi {

  private static final int DEFAULT_SUCCESSFUL_CODE = 0x9000;

  private final byte[] apdu;
  private final Set<Integer> successfulStatusWords;
  private String info;

  /**
   * Builds an APDU request from a raw byte buffer.
   *
   * <p>The default status words list is initialized with the standard successful code 9000h.
   *
   * @param apdu The bytes of the APDU's body.
   * @since 2.0.0
   */
  public ApduRequestAdapter(byte[] apdu) {
    this.apdu = apdu;
    this.successfulStatusWords = new HashSet<Integer>();
    this.successfulStatusWords.add(DEFAULT_SUCCESSFUL_CODE);
  }

  /**
   * Adds a status word to the list of those that should be considered successful for the APDU.
   *
   * <p>Note: initially, the list contains the standard successful status word {@code 9000h}.
   *
   * @param successfulStatusWord A positive int &le; {@code FFFFh}.
   * @return The object instance.
   * @since 2.0.0
   */
  public ApduRequestAdapter addSuccessfulStatusWord(int successfulStatusWord) {
    this.successfulStatusWords.add(successfulStatusWord);
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public Set<Integer> getSuccessfulStatusWords() {
    return successfulStatusWords;
  }

  /**
   * Names the APDU request.
   *
   * <p>This string is dedicated to improve the readability of logs and should therefore only be
   * invoked conditionally (e.g. when log level &gt;= debug).
   *
   * @param info The request name (free text).
   * @return The object instance.
   * @since 2.0.0
   */
  public ApduRequestAdapter setInfo(final String info) {
    this.info = info;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public String getInfo() {
    return info;
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
   * Converts the APDU request into a string where the data is encoded in a json format.
   *
   * @return A not empty String
   * @since 2.0.0
   */
  @Override
  public String toString() {
    return "APDU_REQUEST = " + JsonUtil.toJson(this);
  }
}
