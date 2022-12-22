package com.pos.calypso;

public abstract class CalypsoApduCommandException extends Exception {

  private final CardCommand command;

  private final Integer statusWord;

  /**
   * Constructor allowing to set the error message and the reference to the command
   *
   * @param message the message to identify the exception context (Should not be null).
   * @param command the command.
   * @param statusWord the status word.
   * @since 2.0.0
   */
  protected CalypsoApduCommandException(String message, CardCommand command, Integer statusWord) {
    super(message);
    this.command = command;
    this.statusWord = statusWord;
  }

  /**
   * Gets the command
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  public CardCommand getCommand() {
    return command;
  }

  /**
   * Gets the status word
   *
   * @return A nullable reference
   * @since 2.0.0
   */
  public Integer getStatusWord() {
    return statusWord;
  }
}
