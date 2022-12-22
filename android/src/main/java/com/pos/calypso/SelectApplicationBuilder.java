package com.pos.calypso;

import androidx.annotation.IntDef;

import org.eclipse.keyple.core.util.ApduUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SelectApplicationBuilder extends AbstractCardCommandBuilder<SelectApplicationParser> {

    public static final int SELECT_FIRST_OCCURRENCE_RETURN_FCI = 1;
    public static final int SELECT_NEXT_OCCURRENCE_RETURN_FCI = 10;
    public static final int SELECT_FIRST_OCCURRENCE_DONT_RETURN_FCI = 50;
    public static final int SELECT_NEXT_OCCURRENCE_DONT_RETURN_FCI = 100;

    @IntDef({SELECT_FIRST_OCCURRENCE_RETURN_FCI, SELECT_NEXT_OCCURRENCE_RETURN_FCI,
            SELECT_FIRST_OCCURRENCE_DONT_RETURN_FCI, SELECT_NEXT_OCCURRENCE_DONT_RETURN_FCI})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SelectMode {}

    private static final CalypsoCardCommand command = CalypsoCardCommand.SELECT_APPLICATION;

    public SelectApplicationBuilder(@SelectMode int selectMode) {
        super(command);

        byte cla = (byte) 0x00;
        byte p1 = (byte) 0x04;
        byte p2;

        if(selectMode == SELECT_FIRST_OCCURRENCE_RETURN_FCI)
            p2 = (byte) 0x00;
        else if(selectMode == SELECT_NEXT_OCCURRENCE_RETURN_FCI)
            p2 = (byte) 0x02;
        else if(selectMode == SELECT_FIRST_OCCURRENCE_DONT_RETURN_FCI)
            p2 = (byte) 0x0C;
        else if(selectMode == SELECT_NEXT_OCCURRENCE_DONT_RETURN_FCI)
            p2 = (byte) 0x0E;
        else
            throw new IllegalArgumentException("SelectMode value not valid!");

        setApduRequest(new ApduRequestAdapter(ApduUtil.build(cla, command.getInstructionByte(),
                p1, p2, Calypso.AID, null)));
    }

    @Override
    public SelectApplicationParser createResponseParser(ApduResponseApi apduResponse) {
        return new SelectApplicationParser(apduResponse, this);
    }

    @Override
    public boolean isSessionBufferUsed() {
        return false;
    }
}
