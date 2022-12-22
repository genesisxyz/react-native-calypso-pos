package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public final class SamDigestCloseBuilder extends AbstractSamCommandBuilder<SamDigestCloseParser> {

  /** The command. */
  private static final CalypsoSamCommand command = CalypsoSamCommand.DIGEST_CLOSE;

  /**
   * Instantiates a new SamDigestCloseBuilder .
   *
   * @param expectedResponseLength the expected response length.
   * @throws IllegalArgumentException - if the expected response length is wrong.
   * @since 2.0.0
   */
  public SamDigestCloseBuilder(byte expectedResponseLength) {
    super(command);
    if (expectedResponseLength != 0x04 && expectedResponseLength != 0x08) {
      throw new IllegalArgumentException(
          String.format("Bad digest length! Expected 4 or 8, got %s", expectedResponseLength));
    }

    byte cla = defaultClassByte;
    byte p1 = (byte) 0x00;
    byte p2 = (byte) 0x00;

    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(
                cla, command.getInstructionByte(), p1, p2, null, expectedResponseLength)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public SamDigestCloseParser createResponseParser(ApduResponseApi apduResponse) {
    return new SamDigestCloseParser(apduResponse, this);
  }
}
