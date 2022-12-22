package com.pos.calypso;

import java.util.HashMap;
import java.util.Map;

public final class SamGetChallengeParser extends AbstractSamResponseParser {

  private static final Map<Integer, StatusProperties> STATUS_TABLE;

  static {
    Map<Integer, StatusProperties> m =
        new HashMap<Integer, StatusProperties>(AbstractSamResponseParser.STATUS_TABLE);
    m.put(0x6700, new StatusProperties("Incorrect Le.", CalypsoSamIllegalParameterException.class));
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
   * Instantiates a new SamGetChallengeParser .
   *
   * @param response of the SamGetChallengeBuilder.
   * @param builder the reference to the builder that created this parser.
   * @since 2.0.0
   */
  public SamGetChallengeParser(ApduResponseApi response, SamGetChallengeBuilder builder) {
    super(response, builder);
  }

  /**
   * Gets the challenge.
   *
   * @return the challenge
   * @since 2.0.0
   */
  public byte[] getChallenge() {
    return isSuccessful() ? response.getDataOut() : null;
  }
}
