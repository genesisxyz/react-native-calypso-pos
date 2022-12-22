package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public final class SamUnlockBuilder extends AbstractSamCommandBuilder<SamUnlockParser> {
  /** The command reference. */
  private static final CalypsoSamCommand command = CalypsoSamCommand.UNLOCK;

  /**
   * CalypsoSamCardSelectorBuilder constructor
   *
   * @param unlockData the unlock data.
   * @since 2.0.0
   */
  public SamUnlockBuilder(byte[] unlockData) {
    super(command);

    byte cla = defaultClassByte;
    byte p1 = (byte) 0x00;
    byte p2 = (byte) 0x00;

    if (unlockData == null) {
      throw new IllegalArgumentException("Unlock data null!");
    }

    if (unlockData.length != 8 && unlockData.length != 16) {
      throw new IllegalArgumentException("Unlock data should be 8 ou 16 bytes long!");
    }

    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(cla, command.getInstructionByte(), p1, p2, unlockData, null)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public SamUnlockParser createResponseParser(ApduResponseApi apduResponse) {
    return new SamUnlockParser(apduResponse, this);
  }
}
