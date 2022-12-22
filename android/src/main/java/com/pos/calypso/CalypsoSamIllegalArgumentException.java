package com.pos.calypso;

public final class CalypsoSamIllegalArgumentException extends CalypsoSamCommandException {

  /**
   * (package-private)<br>
   *
   * @param message the message to identify the exception context.
   * @param command the Calypso SAM command.
   * @since 2.0.0
   */
  CalypsoSamIllegalArgumentException(String message, CalypsoSamCommand command) {
    super(message, command, null);
  }
}
