package com.pos.calypso;

import java.util.HashMap;
import java.util.Map;

public class AbstractApduResponseParser {

    protected static final Map<Integer, StatusProperties> STATUS_TABLE;

    static {
        HashMap<Integer, StatusProperties> m = new HashMap<>();
        m.put(0x9000, StatusProperties.success("Success"));
        STATUS_TABLE = m;
    }

    protected final ApduResponseApi response;

    protected final AbstractApduCommandBuilder builder;

    public AbstractApduResponseParser(ApduResponseApi response, AbstractApduCommandBuilder builder) {
        this.response = response;
        this.builder = builder;
    }

    public ApduResponseApi getResponse() {
        return response;
    }

    public AbstractApduCommandBuilder getBuilder() {
        return builder;
    }

    protected Map<Integer, StatusProperties> getStatusTable() {
        return STATUS_TABLE;
    }

    protected CalypsoApduCommandException buildCommandException(
            Class<? extends CalypsoApduCommandException> exceptionClass,
            String message,
            CardCommand commandRef,
            Integer statusWord) {
        return new CardCommandUnknownStatusException(message, commandRef, statusWord);
    }

    protected CardCommand getCommandRef() {
        return builder != null ? builder.getCommandRef() : null;
    }

    public final boolean isSuccess() {
        StatusProperties props = getStatusWordProperties();
        return props != null && props.isSuccessful();
    }

    private StatusProperties getStatusWordProperties() {
        return getStatusTable().get(response.getStatusWord());
    }

    public final String getStatusInformation() {
        StatusProperties statusProperties = getStatusWordProperties();
        return statusProperties != null ? statusProperties.getInformation() : null;
    }

    public void checkStatus() throws CalypsoApduCommandException {
        StatusProperties props = getStatusWordProperties();
        if (props != null && props.isSuccessful()) {
            return;
        }

        // status word is not referenced, or not successful.

        // exception class
        Class<? extends CalypsoApduCommandException> exceptionClass =
                props != null ? props.getExceptionClass() : null;

        // message
        String message = props != null ? props.getInformation() : "Unknown status";

        // command reference
        CardCommand commandRef = getCommandRef();

        // status word
        Integer statusWord = response.getStatusWord();

        // Throw the exception
        throw buildCommandException(exceptionClass, message, commandRef, statusWord);
    }

    public boolean isSuccessful() {
        StatusProperties props = getStatusWordProperties();
        return props != null && props.isSuccessful();
    }

    protected static class StatusProperties {

        public static StatusProperties success(String information) {
            return new StatusProperties(information);
        }

        public static StatusProperties failure(String information,
                                               Class<? extends CalypsoApduCommandException> exceptionClass) {
            return new StatusProperties(information, exceptionClass);
        }

        private final String information;
        private final boolean successful;
        private final Class<? extends CalypsoApduCommandException> exceptionClass;

        public StatusProperties(String information) {
            this.information = information;
            this.successful = true;
            this.exceptionClass = null;
        }

        public StatusProperties(
                String information, Class<? extends CalypsoApduCommandException> exceptionClass) {
            this.information = information;
            this.successful = exceptionClass == null;
            this.exceptionClass = exceptionClass;
        }

        public String getInformation() {
            return information;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public Class<? extends CalypsoApduCommandException> getExceptionClass() {
            return exceptionClass;
        }
    }
}
