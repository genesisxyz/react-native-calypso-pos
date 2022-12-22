package com.pos.calypso;

abstract class CalypsoSamCommandException extends CalypsoApduCommandException {

  /**
   * @param message the message to identify the exception context.
   * @param command the Calypso SAM command.
   * @param statusWord the status word (optional).
   * @since 2.0.0
   */
  protected CalypsoSamCommandException(
      String message, CalypsoSamCommand command, Integer statusWord) {
    super(message, command, statusWord);
  }
}
