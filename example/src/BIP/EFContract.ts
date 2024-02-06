import { byteArrayFromHex, File } from './File';

export namespace EFContract {
  export class Record extends File {
    record: Uint8Array;

    RECORD_BYTE_LENGTH = 0x1d;
    static SFI = 0x09;

    /**
     * Identifies the company issuing the contract
     * @private
     */
    private static AGENCY_CODE_RANGE = [0x0, 0x1] as const;

    /**
     * Identifies the format used to encode the data
     */
    private static ID_FORMAT_RANGE = [0x1, 0x2] as const;

    /**
     * RFU
     */
    private static RFU_RANGE = [0x2, 0x3] as const;

    /**
     * ATI tariff code
     */
    private static ADDITIONAL_TARIFF_CODE_RANGE = [0x3, 0x4] as const;

    /**
     * Code for the sales list of securities for the individual company
     */
    private static TARIFF_CODE = [0x4, 0x6] as const;

    /**
     * Number in binary format
     */
    private static CONTRACT_ISSUE_SERIAL_NUMBER_RANGE = [0x6, 0x9] as const;

    /**
     * Start date of contract validity or validity starting from the first validation stamp
     */
    private static VALIDITY_PERIOD_START_RANGE = [0x9, 0x0c] as const;

    /**
     * End date of contract validity or validity ending from the first validation
     */
    private static VALIDITY_PERIOD_END_RANGE = [0x0c, 0x0f] as const;

    /**
     * Weekly validity days, validity on holidays
     */
    private static CONTRACT_VALIDITY_DAYS_RANGE = [0x0f, 0x10] as const;

    /**
     * Serial number of the SAM that issued the ticket
     */
    private static SAM_SERIAL_NUMBER_RANGE = [0x10, 0x14] as const;

    /**
     * Value of the counter of the SAM that issued the ticket
     */
    private static SAM_COUNTER_RANGE = [0x14, 0x17] as const;

    /**
     * The supplementary field "contract extension" indicates the presence of additional records associated with the current one,
     * containing descriptive information about the contract.
     * The contract records in the file are numbered from 1 to 8.
     * A contract can be encoded across multiple records; in this case,
     * the following supplementary descriptive field indicates the existing concatenation between the records associated with the contract.
     */
    private static CONTRACT_EXTENSION_TAG = [0x17, 0x19] as const;

    /**
     * TODO: where is this documented?
     * Vedi nota tecnica: BIP_firma_e_verifica
     */
    private static MAC_CONTRACT_RAGE = [0x19, 0x1d] as const;

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
