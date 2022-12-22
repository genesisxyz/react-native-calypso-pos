package com.pos.calypso;

import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.BerTlvUtil;

import java.util.HashMap;
import java.util.Map;

public class CardSelectFileParser extends AbstractCardResponseParser {

    private static final Map<Integer, StatusProperties> STATUS_TABLE;

    private static final int TAG_PROPRIETARY_INFORMATION = 0x85;

    static {
        Map<Integer, StatusProperties> m =
                new HashMap<Integer, StatusProperties>(AbstractCardResponseParser.STATUS_TABLE);
        m.put(0x6700,
                new StatusProperties("Lc value not supported.", CardIllegalParameterException.class));
        m.put(0x6A82, new StatusProperties("File not found.", CardDataAccessException.class));
        m.put(0x6119, new StatusProperties("Correct execution (ISO7816 T=0).", null));
        STATUS_TABLE = m;
    }

    private byte[] proprietaryInformation;

    public CardSelectFileParser(ApduResponseApi response, CardSelectFileBuilder builder) {
        super(response, builder);
        proprietaryInformation = null;
    }

    @Override
    protected Map<Integer, StatusProperties> getStatusTable() {
        return STATUS_TABLE;
    }

    public byte[] getProprietaryInformation() {
        if (proprietaryInformation == null) {
            Map<Integer, byte[]> tags = BerTlvUtil.parseSimple(response.getDataOut(), true);
            proprietaryInformation = tags.get(TAG_PROPRIETARY_INFORMATION);
            if (proprietaryInformation == null) {
                throw new IllegalStateException("Proprietary information: tag not found.");
            }
            Assert.getInstance().isEqual(proprietaryInformation.length, 23,
                    "proprietaryInformation");
        }
        return proprietaryInformation;
    }
}
