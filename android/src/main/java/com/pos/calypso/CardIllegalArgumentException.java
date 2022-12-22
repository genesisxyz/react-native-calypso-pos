package com.pos.calypso;

public final class CardIllegalArgumentException extends CardCommandException {

  /**
   * (package-private)<br>
   *
   * @param message the message to identify the exception context.
   * @param command the Calypso card command.
   * @since 2.0.0
   */
  CardIllegalArgumentException(String message, CalypsoCardCommand command) {
    super(message, command, null);
  }
}
