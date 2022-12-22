package com.pos.calypso;

abstract class AbstractCardCommandBuilder<T extends AbstractCardResponseParser>
    extends AbstractApduCommandBuilder {

  /**
   * Constructor dedicated for the building of referenced Calypso commands
   *
   * @param commandRef a command reference from the Calypso command table.
   * @since 2.0.0
   */
  protected AbstractCardCommandBuilder(CalypsoCardCommand commandRef) {
    super(commandRef);
  }

  /**
   * Create the response parser matching the builder
   *
   * @param apduResponse the response data from the the card.
   * @return An {@link AbstractCardResponseParser}
   */
  public abstract T createResponseParser(ApduResponseApi apduResponse);

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CalypsoCardCommand getCommandRef() {
    return (CalypsoCardCommand) commandRef;
  }

  /**
   * Indicates if the session buffer is used when executing this command.
   *
   * <p>Allows the management of the overflow of this buffer.
   *
   * @return True if this command uses the session buffer
   * @since 2.0.0
   */
  public abstract boolean isSessionBufferUsed();

  public byte[] getApdu() {
    return getApduRequest().getApdu();
  }
}
