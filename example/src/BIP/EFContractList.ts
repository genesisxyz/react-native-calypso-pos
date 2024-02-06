import { byteArrayFromHex, File } from './File';

export namespace EFContractList {
  export class Record extends File {
    record: Uint8Array;

    RECORD_BYTE_LENGTH = 0x1d;
    static SFI = 0x1e;

    private static ID_FORMAT_RANGE = [0x0, 0x1] as const;

    private static CONTRACT_1_TYPE_RANGE = [0x1, 0x4] as const;
    private static CONTRACT_2_TYPE_RANGE = [0x4, 0x7] as const;
    private static CONTRACT_3_TYPE_RANGE = [0x7, 0xa] as const;
    private static CONTRACT_4_TYPE_RANGE = [0x7, 0xd] as const;
    private static CONTRACT_5_TYPE_RANGE = [0xd, 0x10] as const;
    private static CONTRACT_6_TYPE_RANGE = [0x10, 0x13] as const;
    private static CONTRACT_7_TYPE_RANGE = [0x13, 0x16] as const;
    private static CONTRACT_8_TYPE_RANGE = [0x16, 0x19] as const;

    private static RFU_RANGE = [0x19, 0x1d] as const;

    constructor(record: Uint8Array | number[]) {
      super();
      if (record.length !== this.RECORD_BYTE_LENGTH) {
        throw Error(`Wrong record byte length, must be exactly ${this.RECORD_BYTE_LENGTH} bytes`);
      }
      this.record = new Uint8Array(record);
    }

    public static fromData(data: object) {
      const newRecord = '00';

      return new this(byteArrayFromHex(newRecord));
    }

    private get recordParsed() {
      return {};
    }
  }
}
