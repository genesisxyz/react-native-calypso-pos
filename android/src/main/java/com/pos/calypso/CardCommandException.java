
package com.pos.calypso;

public abstract class CardCommandException extends CalypsoApduCommandException {

  /**
   * @param message the message to identify the exception context.
   * @param command the Calypso card command.
   * @param statusWord the status word (optional).
   * @since 2.0.0
   */
  protected CardCommandException(String message, CalypsoCardCommand command, Integer statusWord) {
    super(message, command, statusWord);
  }
}
