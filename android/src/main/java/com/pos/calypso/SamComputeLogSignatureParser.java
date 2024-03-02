package com.pos.calypso;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class SamComputeLogSignatureParser extends AbstractSamResponseParser {

  private static final Map<Integer, StatusProperties> STATUS_TABLE;

  static {
    Map<Integer, StatusProperties> m =
      new HashMap<Integer, StatusProperties>(AbstractSamResponseParser.STATUS_TABLE);
    m.put(0x6700, new StatusProperties("Incorrect Lc.", CalypsoSamIllegalParameterException.class));
    m.put(
      0x6985,
      new StatusProperties(
        "Preconditions not satisfied: the SAM is locked.",
        CalypsoSamAccessForbiddenException.class));
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


  public byte[] getSignature() {
    if(isSuccessful()) {
      return Arrays.copyOfRange(response.getDataOut(), 0, 2);
//      return response.getDataOut();
    } else
      return null;
  }

  /**
   * Instantiates a new SamSelectDiversifierParser.
   *
   * @param response the response.
   * @param builder the reference to the builder that created this parser.
   * @since 2.0.0
   */
  public SamComputeLogSignatureParser(ApduResponseApi response, SamComputeLogSignatureBuilder builder) {
    super(response, builder);
  }
}
