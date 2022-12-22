package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public final class SamSelectDiversifierBuilder
    extends AbstractSamCommandBuilder<SamSelectDiversifierParser> {

  /** The command. */
  private static final CalypsoSamCommand command = CalypsoSamCommand.SELECT_DIVERSIFIER;

  /**
   * Instantiates a new SamSelectDiversifierBuilder.
   *
   * @param diversifier the application serial number.
   * @throws IllegalArgumentException - if the diversifier is null or has a wrong length
   * @since 2.0.0
   */
  public SamSelectDiversifierBuilder(byte[] diversifier) {
    super(command);
    if (diversifier == null || (diversifier.length != 4 && diversifier.length != 8)) {
      throw new IllegalArgumentException("Bad diversifier value!");
    }

    byte cla = defaultClassByte;
    byte p1 = 0x00;
    byte p2 = 0x00;

    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(cla, command.getInstructionByte(), p1, p2, diversifier, null)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public SamSelectDiversifierParser createResponseParser(ApduResponseApi apduResponse) {
    return new SamSelectDiversifierParser(apduResponse, this);
  }
}
