package com.pos.calypso;

public class CardCommandUnknownStatusException extends CalypsoApduCommandException {

  /**
   * Constructor allowing to set a message, the command and the status word.
   *
   * @param message the message to identify the exception context (Should not be null).
   * @param command the card command (Should not be null).
   * @param statusWord the status word (Should not be null).
   * @since 2.0.0
   */
  public CardCommandUnknownStatusException(
      String message, CardCommand command, Integer statusWord) {
    super(message, command, statusWord);
  }
}
