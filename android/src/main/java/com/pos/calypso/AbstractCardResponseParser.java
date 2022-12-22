package com.pos.calypso;



public abstract class AbstractCardResponseParser extends AbstractApduResponseParser {


    @SuppressWarnings("rawtypes")
    protected AbstractCardResponseParser(
            ApduResponseApi response, AbstractCardCommandBuilder builder) {
        super(response, builder);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0.0
     */
    @SuppressWarnings("unchecked")
    @Override
    public final AbstractCardCommandBuilder<AbstractCardResponseParser> getBuilder() {
        return (AbstractCardCommandBuilder<AbstractCardResponseParser>) super.getBuilder();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0.0
     */
    @Override
    protected final CalypsoApduCommandException buildCommandException(
            Class<? extends CalypsoApduCommandException> exceptionClass,
            String message,
            CardCommand commandRef,
            Integer statusWord) {

        CalypsoApduCommandException e;
        CalypsoCardCommand command = (CalypsoCardCommand) commandRef;
        if (exceptionClass == CardAccessForbiddenException.class) {
            e = new CardAccessForbiddenException(message, command, statusWord);
        } else if (exceptionClass == CardDataAccessException.class) {
            e = new CardDataAccessException(message, command, statusWord);
        } else if (exceptionClass == CardDataOutOfBoundsException.class) {
            e = new CardDataOutOfBoundsException(message, command, statusWord);
        } else if (exceptionClass == CardIllegalArgumentException.class) {
            e = new CardIllegalArgumentException(message, command);
        } else if (exceptionClass == CardIllegalParameterException.class) {
            e = new CardIllegalParameterException(message, command, statusWord);
        } else if (exceptionClass == CardPinException.class) {
            e = new CardPinException(message, command, statusWord);
        } else if (exceptionClass == CardSecurityContextException.class) {
            e = new CardSecurityContextException(message, command, statusWord);
        } else if (exceptionClass == CardSecurityDataException.class) {
            e = new CardSecurityDataException(message, command, statusWord);
        } else if (exceptionClass == CardSessionBufferOverflowException.class) {
            e = new CardSessionBufferOverflowException(message, command, statusWord);
        } else if (exceptionClass == CardTerminatedException.class) {
            e = new CardTerminatedException(message, command, statusWord);
        } else {
            e = new CardUnknownStatusException(message, command, statusWord);
        }
        return e;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0.0
     */
    @Override
    public void checkStatus() throws CardCommandException {
        try {
            super.checkStatus();
        } catch (CalypsoApduCommandException e) {
            throw (CardCommandException) e;
        }
    }
}
