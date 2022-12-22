package com.pos.calypso;

import java.util.HashMap;
import java.util.Map;

public final class SamUnlockParser extends AbstractSamResponseParser {

  private static final Map<Integer, AbstractApduResponseParser.StatusProperties> STATUS_TABLE;

  static {
    Map<Integer, AbstractApduResponseParser.StatusProperties> m =
        new HashMap<Integer, StatusProperties>(AbstractSamResponseParser.STATUS_TABLE);
    m.put(0x6700, new StatusProperties("Incorrect Lc.", CalypsoSamIllegalParameterException.class));
    m.put(
        0x6985,
        new StatusProperties(
            "Preconditions not satisfied (SAM not locked?).",
            CalypsoSamAccessForbiddenException.class));
    m.put(
        0x6988,
        new StatusProperties("Incorrect UnlockData.", CalypsoSamSecurityDataException.class));
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
   * Instantiates a new {@link SamUnlockParser}.
   *
   * @param response the response.
   * @param builder the reference to the builder that created this parser.
   * @since 2.0.0
   */
  public SamUnlockParser(ApduResponseApi response, SamUnlockBuilder builder) {
    super(response, builder);
  }
}
