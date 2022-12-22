package com.pos.calypso;

abstract class AbstractCardOpenSessionBuilder<T extends AbstractCardResponseParser>
    extends AbstractCardCommandBuilder<T> {

  private static boolean isExtendedModeSupported = false;

  /**
   * Instantiates a new AbstractCardOpenSessionBuilder.
   *
   * @throws IllegalArgumentException - if the key index is 0 and rev is 2.4
   * @throws IllegalArgumentException - if the request is inconsistent
   * @since 2.0.0
   */
  AbstractCardOpenSessionBuilder(CalypsoCardCommand command) {
    super(command);
  }

  public static AbstractCardOpenSessionBuilder<AbstractCardOpenSessionParser> create(
      byte debitKeyIndex,
      byte[] sessionTerminalChallenge,
      int sfi,
      int recordNumber) {
    isExtendedModeSupported = false;//calypsoCard.isExtendedModeSupported();
//    switch (calypsoCard.getProductType()) {
//      case PRIME_REVISION_1:
//        return new CardOpenSession10Builder(
//            calypsoCard, debitKeyIndex, sessionTerminalChallenge, sfi, recordNumber);
//      case PRIME_REVISION_2:
//        return new CardOpenSession24Builder(
//            calypsoCard, debitKeyIndex, sessionTerminalChallenge, sfi, recordNumber);
//      case PRIME_REVISION_3:
//      case LIGHT:
//      case BASIC:
        return new CardOpenSession3Builder(debitKeyIndex, sessionTerminalChallenge, sfi, recordNumber);
//      default:
//        throw new IllegalArgumentException(
//            "Product type " + calypsoCard.getProductType() + " isn't supported");
//    }
  }

  /** @return the SFI of the file read while opening the secure session */
  abstract int getSfi();

  /** @return the record number to read */
  abstract int getRecordNumber();

  /**
   * (package-private)<br>
   *
   * @return True if the confidential session mode is supported.
   */
  boolean isIsExtendedModeSupported() {
    return isExtendedModeSupported;
  }
}
