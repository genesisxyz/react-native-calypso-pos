package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public final class SamDigestAuthenticateBuilder
    extends AbstractSamCommandBuilder<SamDigestAuthenticateParser> {

  /** The command. */
  private static final CalypsoSamCommand command = CalypsoSamCommand.DIGEST_AUTHENTICATE;

  /**
   * Instantiates a new SamDigestAuthenticateBuilder .
   *
   * @param signature the signature.
   * @throws IllegalArgumentException - if the signature is null or has a wrong length.
   * @since 2.0.0
   */
  public SamDigestAuthenticateBuilder(byte[] signature) {
    super(command);
    if (signature == null) {
      throw new IllegalArgumentException("Signature can't be null");
    }
    if (signature.length != 4 && signature.length != 8 && signature.length != 16) {
      throw new IllegalArgumentException(
          "Signature is not the right length : length is " + signature.length);
    }
    byte cla = defaultClassByte;
    byte p1 = 0x00;
    byte p2 = (byte) 0x00;

    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(cla, command.getInstructionByte(), p1, p2, signature, null)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public SamDigestAuthenticateParser createResponseParser(ApduResponseApi apduResponse) {
    return new SamDigestAuthenticateParser(apduResponse, this);
  }
}
