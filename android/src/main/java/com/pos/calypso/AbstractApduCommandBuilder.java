package com.pos.calypso;

abstract class AbstractApduCommandBuilder {

  /**
   * The reference field {@link CardCommand} is used to find the type of command concerned when
   * manipulating a list of abstract builder objects. Unfortunately, the diversity of these objects
   * does not allow the use of simple generic methods.
   *
   * @since 2.0.0
   */
  protected final CardCommand commandRef;

  private String name;

  /** The byte array APDU request. */
  private ApduRequestAdapter apduRequest;

  /**
   * (protected)<br>
   * The generic abstract constructor to build an APDU request with a command reference and a byte
   * array.
   *
   * @param commandRef command reference (should not be null).
   * @since 2.0.0
   */
  protected AbstractApduCommandBuilder(CardCommand commandRef) {
    this.commandRef = commandRef;
    this.name = commandRef.getName();
  }

  /**
   * Appends a string to the current name.
   *
   * <p>The subname completes the name of the current command. This method must therefore only be
   * called conditionally (log level &gt;= debug).
   *
   * @param subName the string to append.
   * @since 2.0.0
   */
  public final void addSubName(String subName) {
    if (subName.length() != 0) {
      if (this.name != null) {
        this.name = this.name + " - " + subName;
      } else {
        this.name = subName;
      }
      if (apduRequest != null) {
        this.apduRequest.setInfo(this.name);
      }
    }
  }

  /**
   * Gets {@link CardCommand} the current command identification
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  public CardCommand getCommandRef() {
    return commandRef;
  }

  /**
   * Gets the name of this APDU command if it has been allowed by the log level (see constructor).
   *
   * @return A String (may be null).
   * @since 2.0.0
   */
  public final String getName() {
    return this.name;
  }

  void setApduRequest(ApduRequestAdapter apduRequest) {
    this.apduRequest = apduRequest;
    if (apduRequest != null) {
      this.apduRequest.setInfo(this.name);
    }
  }

  public final ApduRequestAdapter getApduRequest() {
    return apduRequest;
  }
}
