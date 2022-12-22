package com.pos.calypso;

public abstract class AbstractSamCommandBuilder<T extends AbstractSamResponseParser>
    extends AbstractApduCommandBuilder {

  protected byte defaultClassByte = (byte) 0x80;

  protected AbstractSamCommandBuilder(CalypsoSamCommand reference) {
    super(reference);
  }

  /**
   * Create the response parser matching the builder
   *
   * @param apduResponse the response data from the the card.
   * @return an {@link AbstractApduResponseParser}
   */
  public abstract T createResponseParser(ApduResponseApi apduResponse);

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CalypsoSamCommand getCommandRef() {
    return (CalypsoSamCommand) commandRef;
  }

  public byte[] getApdu() {
    return getApduRequest().getApdu();
  }
}
