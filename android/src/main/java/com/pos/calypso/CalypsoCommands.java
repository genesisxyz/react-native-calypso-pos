package com.pos.calypso;

import com.pos.byte_stuff.ByteConvertStringUtil;

import java.util.Locale;

public class CalypsoCommands {
    /**
     * Public plain commands
     */
    public static final String SELECT_APPLICATION = "00 A4 04 00 0E 31 54 49 43 2E 49 43 41 D3 80 12 00 91 01";
    public static final String READ_RECORDS = "00 B2 00 05";
    public static final String CLOSE_SECURE_SESSION = "00 8E 00 00";

    /**
     * Private commands with parameters
     */
    private static final String SELECT_FILE = "00 A4 02 00 02 %S";
    private static final String READ_ONE_RECORD = "00 B2 %S 04";
    private static final String UPDATE_RECORD = "00 DC %S 04 %S %S";
    private static final String WRITE_RECORD = "00 D2 %S 04 %S %S";
    private static final String OPEN_SECURE_SESSION = "00 8A 01 01 04 %S";

    public static String selectFile(String fileLid) {
        return String.format(Locale.ITALIAN, SELECT_FILE, fileLid);
    }

    public static String readRecord(int recordIndex) {
        return String.format(Locale.ITALIAN, READ_ONE_RECORD, ByteConvertStringUtil
                .byteToHexString((byte)recordIndex));
    }

    public static String updateRecord(int recordIndex, String dataToSend) {
        int dataLength = dataToSend.split("\\s+").length;

        return String.format(Locale.ITALIAN, ByteConvertStringUtil
                        .byteToHexString((byte)recordIndex),
                ByteConvertStringUtil
                        .byteToHexString((byte)dataLength), dataToSend);
    }

    public static String writeRecord(int recordIndex, String dataToSend) {
        int dataLength = dataToSend.split("\\s+").length;

        return String.format(Locale.ITALIAN, ByteConvertStringUtil
                        .byteToHexString((byte)recordIndex),
                ByteConvertStringUtil
                        .byteToHexString((byte)dataLength), dataToSend);
    }

    public static String openSecureSession(String samChallenge) {
        return String.format(Locale.ITALIAN, OPEN_SECURE_SESSION, samChallenge);
    }
}
