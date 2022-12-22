package com.pos.calypso;

import java.util.Calendar;

public class Calypso {
    public static final long ZERO_TIME_MILLIS;

    static {
        Calendar zeroTimeCalendar = Calendar.getInstance();
        zeroTimeCalendar.set(Calendar.MINUTE, 0);
        zeroTimeCalendar.set(Calendar.SECOND, 0);
        zeroTimeCalendar.set(Calendar.MILLISECOND, 0);
        zeroTimeCalendar.set(Calendar.HOUR_OF_DAY, 0);
        zeroTimeCalendar.set(Calendar.DAY_OF_MONTH, 1);
        zeroTimeCalendar.set(Calendar.MONTH, 0);
        zeroTimeCalendar.set(Calendar.YEAR, 2005);

        ZERO_TIME_MILLIS = zeroTimeCalendar.getTimeInMillis();
    }

    public static final byte SAM_CHALLENGE_LENGTH_BYTES = 0x04;

    public static final byte SAM_DIGEST_CLOSE_EXPECTED_LENGTH = 4;
    public static final int CARD_EMISSION_TIME_LENGTH_IN_BYTES = 3;

    public static final byte CARD_DATA_FORMAT = 0x05;
    public static final byte CARD_BIP_CIRCUIT = (byte)0xC0;

    public static final byte[] AID = new byte[] { 0x31, 0x54, 0x49, 0x43, 0x2E, 0x49, 0x43, 0x41,
            (byte)0xD3, (byte)0x80, 0x12, 0x00, (byte)0x91, 0x01 };

    public static final byte[] LID_EF_ENVIRONMENT = new byte[] { 0x20, 0x01 };
    public static final int SFI_EF_ENVIRONMENT = 0x07;

    public static final byte STARTUP_INFO_APPLICATION_TYPE = (byte)0x23;
    public static final byte STARTUP_INFO_APPLICATION_SUBTYPE = (byte)0xC0;

    public static final int STARTUP_INFO_APPLICATION_TYPE_INDEX = 2;
    public static final int STARTUP_INFO_APPLICATION_SUBTYPE_INDEX = 3;

    public static final int EF_ENVIRONMENT_DATA_FORMAT_INDEX = 0;
    public static final int EF_ENVIRONMENT_AGENCY_CODE_INDEX = 1;
    public static final int EF_ENVIRONMENT_USER_CODE_INDEX = 2;
    public static final int EF_ENVIRONMENT_USER_CODE_LENGTH = 2;
    public static final int EF_ENVIRONMENT_USER_PROFILE_INDEX = 6;
    public static final int EF_ENVIRONMENT_USER_PROFILE_LENGTH = 2;
    public static final byte[] EF_ENVIRONMENT_USER_PROFILE_MASK = new byte[] { (byte)0xFF, (byte)0xF0};
    public static final int EF_ENVIRONMENT_EXPIRATION_INDEX = 7;
    public static final int EF_ENVIRONMENT_EXPIRATION_LENGTH = 2;
    public static final byte[] EF_ENVIRONMENT_EXPIRATION_MASK = new byte[] { (byte)0x0F, (byte)0xF0};;
    public static final int EF_ENVIRONMENT_CARD_STATUS_INDEX = 8;
    public static final byte EF_ENVIRONMENT_CARD_STATUS_MASK = 0b00001111;
    public static final int EF_ENVIRONMENT_ISSUE_DATE_INDEX = 9;
    public static final int EF_ENVIRONMENT_ISSUE_DATE_LENGTH = 3;
    public static final int EF_ENVIRONMENT_TAX_CODE_INDEX = 12;
    public static final int EF_ENVIRONMENT_TAX_CODE_LENGTH = 16;
    public static final int EF_ENVIRONMENT_CARD_CIRCUIT_INDEX = 28;
}
