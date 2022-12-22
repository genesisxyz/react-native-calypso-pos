package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public final class SamDigestInitBuilder extends AbstractSamCommandBuilder<SamDigestInitParser> {

  /** The command. */
  private static final CalypsoSamCommand command = CalypsoSamCommand.DIGEST_INIT;

  /**
   * Instantiates a new SamDigestInitBuilder.
   *
   * @param verificationMode the verification mode.
   * @param confidentialSessionMode the confidential session mode (rev 3.2).
   * @param workKif from the AbstractCardOpenSessionBuilder response.
   * @param workKvc from the AbstractCardOpenSessionBuilder response.
   * @param digestData all data out from the AbstractCardOpenSessionBuilder response.
   * @throws IllegalArgumentException - if the work key record number
   * @throws IllegalArgumentException - if the digest data is null
   * @throws IllegalArgumentException - if the request is inconsistent
   * @since 2.0.0
   */
  public SamDigestInitBuilder(
      boolean verificationMode,
      boolean confidentialSessionMode,
      byte workKif,
      byte workKvc,
      byte[] digestData) {
    super(command);

    if (workKif == 0x00 || workKvc == 0x00) {
      throw new IllegalArgumentException("Bad key record number, kif or kvc!");
    }
    if (digestData == null) {
      throw new IllegalArgumentException("Digest data is null!");
    }
    byte cla = defaultClassByte;
    byte p1 = 0x00;
    if (verificationMode) {
      p1 = (byte) (p1 + 1);
    }
    if (confidentialSessionMode) {
      p1 = (byte) (p1 + 2);
    }

    byte p2 = (byte) 0xFF;

    byte[] dataIn = new byte[2 + digestData.length];
    dataIn[0] = workKif;
    dataIn[1] = workKvc;
    System.arraycopy(digestData, 0, dataIn, 2, digestData.length);

    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(cla, command.getInstructionByte(), p1, p2, dataIn, null)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public SamDigestInitParser createResponseParser(ApduResponseApi apduResponse) {
    return new SamDigestInitParser(apduResponse, this);
  }
}
