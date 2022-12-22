package com.pos.calypso;

import java.util.HashMap;
import java.util.Map;

public final class SamDigestCloseParser extends AbstractSamResponseParser {

  private static final Map<Integer, StatusProperties> STATUS_TABLE;

  static {
    Map<Integer, StatusProperties> m =
        new HashMap<Integer, StatusProperties>(AbstractSamResponseParser.STATUS_TABLE);
    m.put(
        0x6985,
        new StatusProperties(
            "Preconditions not satisfied.", CalypsoSamAccessForbiddenException.class));
    STATUS_TABLE = m;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  protected Map<Integer, StatusProperties> getStatusTable() {
    return STATUS_TABLE;
  }

  /**
   * Instantiates a new SamDigestCloseParser.
   *
   * @param response from the SamDigestCloseBuilder.
   * @param builder the reference to the builder that created this parser.
   * @since 2.0.0
   */
  public SamDigestCloseParser(ApduResponseApi response, SamDigestCloseBuilder builder) {
    super(response, builder);
  }

  /**
   * Gets the sam signature.
   *
   * @return The sam half session signature
   * @since 2.0.0
   */
  public byte[] getSignature() {
    return isSuccessful() ? response.getDataOut() : null;
  }
}
