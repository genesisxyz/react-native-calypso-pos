package com.pos.calypso;

public final class CardDataOutOfBoundsException extends CardCommandException {

  /**
   * (package-private)<br>
   *
   * @param message the message to identify the exception context.
   * @param command the Calypso card command.
   * @param statusWord the status word.
   * @since 2.0.0
   */
  CardDataOutOfBoundsException(String message, CalypsoCardCommand command, Integer statusWord) {
    super(message, command, statusWord);
  }
}
