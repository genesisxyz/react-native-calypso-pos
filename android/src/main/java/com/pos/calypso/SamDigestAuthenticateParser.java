package com.pos.calypso;

import java.util.HashMap;
import java.util.Map;

public final class SamDigestAuthenticateParser extends AbstractSamResponseParser {

  private static final Map<Integer, StatusProperties> STATUS_TABLE;

  static {
    Map<Integer, StatusProperties> m =
        new HashMap<Integer, StatusProperties>(AbstractSamResponseParser.STATUS_TABLE);
    m.put(0x6700, new StatusProperties("Incorrect Lc.", CalypsoSamIllegalParameterException.class));
    m.put(
        0x6985,
        new StatusProperties(
            "Preconditions not satisfied.", CalypsoSamAccessForbiddenException.class));
    m.put(
        0x6988,
        new StatusProperties("Incorrect signature.", CalypsoSamSecurityDataException.class));
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
   * Instantiates a new SamDigestAuthenticateParser.
   *
   * @param response from the SAM SamDigestAuthenticateBuilder.
   * @param builder the reference to the builder that created this parser.
   * @since 2.0.0
   */
  public SamDigestAuthenticateParser(
      ApduResponseApi response, SamDigestAuthenticateBuilder builder) {
    super(response, builder);
  }
}
