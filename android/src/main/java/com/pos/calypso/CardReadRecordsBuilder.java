package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public final class CardReadRecordsBuilder extends AbstractCardCommandBuilder<CardReadRecordsParser> {

  private static final CalypsoCardCommand command = CalypsoCardCommand.READ_RECORDS;

  /** Indicates if one or multiple records */
  public enum ReadMode {
    /** read one record */
    ONE_RECORD,
    /** read multiple records */
    MULTIPLE_RECORD
  }

  // Construction arguments used for parsing
  private final int sfi;
  private final int firstRecordNumber;
  private final ReadMode readMode;

  /**
   * Instantiates a new read records cmd build.
   *
   * @param sfi the sfi top select.
   * @param firstRecordNumber the record number to read (or first record to read in case of several.
   *     records)
   * @param readMode read mode, requests the reading of one or all the records.
   * @param expectedLength the expected length of the record(s).
   * @throws IllegalArgumentException - if record number &lt; 1
   * @throws IllegalArgumentException - if the request is inconsistent
   * @since 2.0.0
   */
  public CardReadRecordsBuilder(
      int sfi,
      int firstRecordNumber,
      ReadMode readMode,
      int expectedLength) {
    super(command);

    this.sfi = sfi;
    this.firstRecordNumber = firstRecordNumber;
    this.readMode = readMode;

    byte p1 = (byte) firstRecordNumber;
    byte p2 = (sfi == (byte) 0x00) ? (byte) 0x05 : (byte) ((byte) (sfi * 8) + 5);
    if (readMode == ReadMode.ONE_RECORD) {
      p2 = (byte) (p2 - (byte) 0x01);
    }
    byte le = (byte) expectedLength;
    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(
                    (byte)0x00, command.getInstructionByte(), p1, p2, null, le)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CardReadRecordsParser createResponseParser(ApduResponseApi apduResponse) {
    return new CardReadRecordsParser(apduResponse, this);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This command doesn't modify the contents of the card and therefore doesn't uses the session
   * buffer.
   *
   * @return false
   * @since 2.0.0
   */
  @Override
  public boolean isSessionBufferUsed() {
    return false;
  }

  /**
   * @return the SFI of the accessed file
   * @since 2.0.0
   */
  public int getSfi() {
    return sfi;
  }

  /**
   * @return the number of the first record to read
   * @since 2.0.0
   */
  public int getFirstRecordNumber() {
    return firstRecordNumber;
  }

  /**
   * @return the readJustOneRecord flag
   * @since 2.0.0
   */
  public ReadMode getReadMode() {
    return readMode;
  }
}
