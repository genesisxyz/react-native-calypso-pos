package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public final class CardAppendRecordBuilder extends AbstractCardCommandBuilder<CardAppendRecordParser> {

  /** The command. */
  private static final CalypsoCardCommand command = CalypsoCardCommand.APPEND_RECORD;

  /* Construction arguments */
  private final int sfi;
  private final byte[] data;

  /**
   * Instantiates a new CardUpdateRecordBuilder.
   *
   * @param sfi the sfi to select.
   * @param newRecordData the new record data to write.
   * @throws IllegalArgumentException - if the command is inconsistent
   * @since 2.0.0
   */
  public CardAppendRecordBuilder(byte sfi, byte[] newRecordData) {
    super(command);
    byte cla = 0x00;

    this.sfi = sfi;
    this.data = newRecordData;

    byte p1 = (byte) 0x00;
    byte p2 = (sfi == 0) ? (byte) 0x00 : (byte) (sfi * 8);

    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(cla, command.getInstructionByte(), p1, p2, newRecordData, null)));

  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CardAppendRecordParser createResponseParser(ApduResponseApi apduResponse) {
    return new CardAppendRecordParser(apduResponse, this);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This command modified the contents of the card and therefore uses the session buffer.
   *
   * @return True
   * @since 2.0.0
   */
  @Override
  public boolean isSessionBufferUsed() {
    return true;
  }

  /**
   * @return The SFI of the accessed file
   * @since 2.0.0
   */
  public int getSfi() {
    return sfi;
  }

  /**
   * @return The data sent to the card
   * @since 2.0.0
   */
  public byte[] getData() {
    return data;
  }
}
