package com.pos.calypso;

import android.util.Log;

import org.eclipse.keyple.core.util.BerTlvUtil;

import java.util.Arrays;
import java.util.Map;

public class SelectApplicationParser extends AbstractCardResponseParser {

    public static final String TAG = "hdvk";

    private static final int APPLICATION_TYPE_SI_INDEX = 2;
    private static final int APPLICATION_SUBTYPE_SI_INDEX = 3;
    private static final int MAX_EDITABLE_BYTES_SI_INDEX = 0;

    /* BER-TLV tags definitions */
    private static final int TAG_DF_NAME = 0x84;
    private static final int TAG_APPLICATION_SERIAL_NUMBER = 0xC7;
    private static final int TAG_DISCRETIONARY_DATA = 0x53;

    /** attributes result of th FCI parsing */
    private boolean isDfInvalidated = false;

    private boolean isValidCalypsoFCI = false;

    private byte[] dfName = null;
    private byte[] applicationSN = null;
    private byte[] startupInfo;
    private byte maxBytesEditable;
    private byte applicationType;
    private byte applicationSubtype;

    public SelectApplicationParser(ApduResponseApi response, SelectApplicationBuilder builder) {
        super(response, builder);

        Map<Integer, byte[]> tags;

        try {
            final byte[] responseData = response.getDataOut();
            tags = BerTlvUtil.parseSimple(responseData, true);
            dfName = tags.get(TAG_DF_NAME);
            if (dfName == null) {
                Log.e(TAG, "DF name tag (84h) not found.");
                return;
            }
            if (dfName.length < 5 || dfName.length > 16) {
                Log.e(TAG, "Invalid DF name length: {}. Should be between 5 and 16.");
                return;
            }

            applicationSN = tags.get(TAG_APPLICATION_SERIAL_NUMBER);
            if (applicationSN == null) {
                Log.e(TAG, "Serial Number tag (C7h) not found.");
                return;
            }
            if (applicationSN.length != 8) {
                Log.e(TAG,
                        "Invalid application serial number length: {}. Should be 8.");
                return;
            }

            startupInfo = tags.get(TAG_DISCRETIONARY_DATA);
            if (startupInfo == null) {
                Log.e(TAG, "Discretionary data tag (53h) not found.");
                return;
            }
            startupInfo = Arrays.copyOfRange(startupInfo, 0, startupInfo.length);
            if (startupInfo.length < 7) {
                Log.e(TAG, "Invalid startup info length: {}. Should be >= 7.");
                return;
            }

            maxBytesEditable = startupInfo[MAX_EDITABLE_BYTES_SI_INDEX];
            applicationType = startupInfo[APPLICATION_TYPE_SI_INDEX];
            applicationSubtype = startupInfo[APPLICATION_SUBTYPE_SI_INDEX];

            /* all 3 main fields were retrieved */
            isValidCalypsoFCI = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getStartupInfoRawData() {
        return startupInfo;
    }

    public byte getMaxBytesEditable() {
        return maxBytesEditable;
    }

    public byte getApplicationType() {
        return applicationType;
    }

    public byte getApplicationSubtype() {
        return applicationSubtype;
    }

    public byte[] getDfName() {
        return dfName;
    }

    public byte[] getApplicationSN() {
        return applicationSN;
    }
}
