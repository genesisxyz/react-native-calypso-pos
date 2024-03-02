package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public final class SamComputeLogSignatureBuilder
  extends AbstractSamCommandBuilder<SamComputeLogSignatureParser> {

  /** The command. */
  private static final CalypsoSamCommand command = CalypsoSamCommand.COMPUTE_SIGNATURE;


  public SamComputeLogSignatureBuilder(byte kif, byte kvc, byte[] cardSerialNumber, byte[] log) {
    super(command);

    byte cla = (byte)0x80;
    byte p1 = (byte)0x9E;
    byte p2 = (byte)0x9A;

    final byte[] data = new byte[39];
    data[0] = (byte)0xFF;
    data[1] = kif;
    data[2] = kvc;
    data[3] = (byte)0x82;

    System.arraycopy(cardSerialNumber, 0, data, 4, 8);
    System.arraycopy(log, 0, data, 12, 27);

    setApduRequest(
      new ApduRequestAdapter(
        ApduUtil.build(cla, command.getInstructionByte(), p1, p2, data, null)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public SamComputeLogSignatureParser createResponseParser(ApduResponseApi apduResponse) {
    return new SamComputeLogSignatureParser(apduResponse, this);
  }
}
