package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;
import org.eclipse.keyple.core.util.ByteArrayUtil;

public final class CardCloseSessionBuilder extends AbstractCardCommandBuilder<CardCloseSessionParser> {

  /** The command. */
  private static final CalypsoCardCommand command = CalypsoCardCommand.CLOSE_SESSION;

  /**
   * Instantiates a new CardCloseSessionBuilder depending on the revision of the card.
   *
   * @param ratificationAsked the ratification asked.
   * @param terminalSessionSignature the sam half session signature.
   * @throws IllegalArgumentException - if the signature is null or has a wrong length
   * @throws IllegalArgumentException - if the command is inconsistent
   * @since 2.0.0
   */
  public CardCloseSessionBuilder(boolean ratificationAsked, byte[] terminalSessionSignature) {
    super(command);

    // The optional parameter terminalSessionSignature could contain 4 or 8
    // bytes.
    if (terminalSessionSignature != null
        && terminalSessionSignature.length != 4
        && terminalSessionSignature.length != 8) {
      throw new IllegalArgumentException(
          "Invalid terminal sessionSignature: " + ByteArrayUtil.toHex(terminalSessionSignature));
    }

    byte p1 = ratificationAsked ? (byte) 0x80 : (byte) 0x00;
    /*
     * case 4: this command contains incoming and outgoing data. We define le = 0, the actual
     * length will be processed by the lower layers.
     */
    byte le = 0;

    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(
                (byte)0x00,
                command.getInstructionByte(),
                p1,
                (byte) 0x00,
                terminalSessionSignature,
                le)));
  }

  /**
   * Instantiates a new CardCloseSessionBuilder based on the revision of the card to generate an
   * abort session command (Close Secure Session with p1 = p2 = lc = 0).
   *
   * @since 2.0.0
   */
  public CardCloseSessionBuilder() {
    super(command);

    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(
                    (byte)0x00,
                command.getInstructionByte(),
                (byte) 0x00,
                (byte) 0x00,
                null,
                (byte) 0)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CardCloseSessionParser createResponseParser(ApduResponseApi apduResponse) {
    return new CardCloseSessionParser(apduResponse, this);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This command can't be executed in session and therefore doesn't uses the session buffer.
   *
   * @return False
   * @since 2.0.0
   */
  @Override
  public boolean isSessionBufferUsed() {
    return false;
  }
}
