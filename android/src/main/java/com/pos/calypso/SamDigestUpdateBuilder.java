package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public final class SamDigestUpdateBuilder extends AbstractSamCommandBuilder<SamDigestUpdateParser> {

  /** The command reference. */
  private static final CalypsoSamCommand command = CalypsoSamCommand.DIGEST_UPDATE;

  /**
   * Instantiates a new SamDigestUpdateBuilder.
   *
   * @param encryptedSession the encrypted session flag, true if encrypted.
   * @param digestData all bytes from command sent by the card or response from the command.
   * @throws IllegalArgumentException - if the digest data is null or has a length &gt; 255
   * @since 2.0.0
   */
  public SamDigestUpdateBuilder(boolean encryptedSession, byte[] digestData) {
    super(command);

    byte cla = defaultClassByte;
    byte p1 = (byte) 0x00;
    byte p2 = encryptedSession ? (byte) 0x80 : (byte) 0x00;

    if (digestData == null || digestData.length > 255) {
      throw new IllegalArgumentException("Digest data null or too long!");
    }

    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(cla, command.getInstructionByte(), p1, p2, digestData, null)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public SamDigestUpdateParser createResponseParser(ApduResponseApi apduResponse) {
    return new SamDigestUpdateParser(apduResponse, this);
  }
}
