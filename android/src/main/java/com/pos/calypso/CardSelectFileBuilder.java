package com.pos.calypso;

import org.eclipse.keyple.core.util.ApduUtil;

public class CardSelectFileBuilder extends AbstractCardCommandBuilder<CardSelectFileParser> {

    private static final CalypsoCardCommand command = CalypsoCardCommand.SELECT_FILE;

    private final byte[] path;
    private final SelectFileControl selectFileControl;

    public CardSelectFileBuilder(SelectFileControl selectFileControl) {
        super(command);

        this.path = null;
        this.selectFileControl = selectFileControl;

        byte cla = (byte) 0x00;
        byte p1;
        byte p2;
        byte[] selectData = new byte[] {0x00, 0x00};
        switch (selectFileControl) {
            case FIRST_EF:
                p1 = (byte) 0x02;
                p2 = (byte) 0x00;
                break;
            case NEXT_EF:
                p1 = (byte) 0x02;
                p2 = (byte) 0x02;
                break;
            case CURRENT_DF:
                p1 = (byte) 0x09;
                p2 = (byte) 0x00;
                break;
            default:
                throw new IllegalStateException(
                        "Unsupported selectFileControl parameter " + selectFileControl.name());
        }

        setApduRequest(
                new ApduRequestAdapter(
                        ApduUtil.build(cla, command.getInstructionByte(), p1, p2, selectData, (byte) 0x00)));
    }

    public CardSelectFileBuilder(byte[] selectionPath) {
        super(command);

        this.path = selectionPath;
        this.selectFileControl = null;

        setApduRequest(
                new ApduRequestAdapter(
                        ApduUtil.build(
                                (byte)0x00,
                                command.getInstructionByte(),
                                (byte) 0x09,
                                (byte) 0x00,
                                selectionPath,
                                (byte) 0x00)));

    }

    @Override
    public CardSelectFileParser createResponseParser(ApduResponseApi apduResponse) {
        return new CardSelectFileParser(apduResponse, this);
    }

    @Override
    public boolean isSessionBufferUsed() {
        return false;
    }

    public byte[] getPath() {
        return path;
    }

    public SelectFileControl getSelectFileControl() {
        return selectFileControl;
    }
}
