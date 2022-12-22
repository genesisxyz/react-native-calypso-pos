package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public final class SamGetChallengeBuilder extends AbstractSamCommandBuilder<SamGetChallengeParser> {

  /** The command reference. */
  private static final CalypsoSamCommand command = CalypsoSamCommand.GET_CHALLENGE;

  /**
   * Instantiates a new SamGetChallengeBuilder.
   *
   * @param revision of the SAM (SAM).
   * @param expectedResponseLength the expected response length.
   * @throws IllegalArgumentException - if the expected response length has wrong value.
   * @since 2.0.0
   */
  public SamGetChallengeBuilder(byte expectedResponseLength) {
    super(command);
    if (expectedResponseLength != 0x04 && expectedResponseLength != 0x08) {
      throw new IllegalArgumentException(
          String.format("Bad challenge length! Expected 4 or 8, got %s", expectedResponseLength));
    }
    byte cla = defaultClassByte;
    byte p1 = 0x00;
    byte p2 = 0x00;

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
  public SamGetChallengeParser createResponseParser(ApduResponseApi apduResponse) {
    return new SamGetChallengeParser(apduResponse, this);
  }
}
