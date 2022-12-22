package com.pos.calypso;

public final class CalypsoSamAccessForbiddenException extends CalypsoSamCommandException {

  /**
   * (package-private)<br>
   *
   * @param message the message to identify the exception context.
   * @param command the Calypso SAM command.
   * @param statusWord the status word.
   * @since 2.0.0
   */
  CalypsoSamAccessForbiddenException(
      String message, CalypsoSamCommand command, Integer statusWord) {
    super(message, command, statusWord);
  }
}
