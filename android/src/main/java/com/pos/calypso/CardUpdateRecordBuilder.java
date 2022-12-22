package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public final class CardUpdateRecordBuilder extends AbstractCardCommandBuilder<CardUpdateRecordParser> {

  /** The command. */
  private static final CalypsoCardCommand command = CalypsoCardCommand.UPDATE_RECORD;

  /* Construction arguments */
  private final int sfi;
  private final int recordNumber;
  private final byte[] data;

  /**
   * Instantiates a new CardUpdateRecordBuilder.
   *
   * @param sfi the sfi to select.
   * @param recordNumber the record number to update.
   * @param newRecordData the new record data to write.
   * @throws IllegalArgumentException - if record number is &lt; 1
   * @throws IllegalArgumentException - if the request is inconsistent
   * @since 2.0.0
   */
  public CardUpdateRecordBuilder(byte sfi, int recordNumber, byte[] newRecordData) {
    super(command);

    byte cla = 0x00;
    this.sfi = sfi;
    this.recordNumber = recordNumber;
    this.data = newRecordData;

    byte p2 = (sfi == 0) ? (byte) 0x04 : (byte) ((byte) (sfi * 8) + 4);

    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(
                cla, command.getInstructionByte(), (byte) recordNumber, p2, newRecordData, null)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CardUpdateRecordParser createResponseParser(ApduResponseApi apduResponse) {
    return new CardUpdateRecordParser(apduResponse, this);
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
   * @return The number of the accessed record
   * @since 2.0.0
   */
  public int getRecordNumber() {
    return recordNumber;
  }

  /**
   * @return The data sent to the card
   * @since 2.0.0
   */
  public byte[] getData() {
    return data;
  }
}
