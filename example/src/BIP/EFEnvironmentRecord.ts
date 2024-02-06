import { add, isFuture, isPast } from 'date-fns';
import { Pos } from 'react-native-pos';

import { byteArrayFromHex, File, getBytes } from './File';

export namespace EFEnvironment {
  export class Record extends File {
    record: Uint8Array;

    RECORD_BYTE_LENGTH = 29;

    static SFI = 0x07;

    private static DATA_FORMAT_RANGE = [0, 1] as const;

    private static AGENCY_CODE_RANGE = [1, 2] as const;

    private static USER_CODE_RANGE = [2, 6] as const;

    private static USER_PROFILE_RANGE = [6, 7.5] as const;

    private static PROFILE_DURATION_RANGE = [7.5, 8.5] as const;

    private static CARD_STATUS_RANGE = [8.5, 9] as const;

    private static EMISSION_DATE_RANGE = [9, 12] as const;

    private static TAX_CODE_RANGE = [12, 28] as const;

    private static CARD_CIRCUIT_RANGE = [28, 29] as const;

    constructor(record: Uint8Array | number[]) {
      super();
      if (record.length !== this.RECORD_BYTE_LENGTH) {
        throw Error(`Wrong record byte length, must be exactly ${this.RECORD_BYTE_LENGTH} bytes`);
      }
      this.record = new Uint8Array(record);
    }

    public static fromData(data: {
      dataFormat: EFEnvironment.CardDataFormat;
      agencyCode: string;
      userCode: number;
      /**
       * exactly 3 bytes
       */
      userProfile: string;
      /**
       * months
       */
      profileDuration?: number;
      cardStatus: EFEnvironment.CardStatus;
      /**
       * timestamp
       */
      emissionDate?: number;
      /**
       * 16 bytes
       */
      taxCode: string;
      cardCircuit: EFEnvironment.CardCircuit;
    }) {
      const [userCodeOffset, userCodeLimit] = this.USER_CODE_RANGE;
      const userCodeNibbleLength = (userCodeLimit - userCodeOffset) * 2;

      const { dataFormat, agencyCode, userProfile, cardStatus, cardCircuit } = data;

      const userCode = data.userCode.toString(16).padStart(userCodeNibbleLength, '0');

      const taxCode = data.taxCode
        .split('')
        .map((e) => e.charCodeAt(0).toString(16).padStart(2, '0'))
        .join('');

      const [profileDurationOffset, profileDurationLimit] = this.PROFILE_DURATION_RANGE;
      const profileDurationNibbleLength = (profileDurationLimit - profileDurationOffset) * 2;

      const profileDuration = data.profileDuration
        ? data.profileDuration.toString(16).padStart(profileDurationNibbleLength, '0')
        : ''.padEnd(profileDurationNibbleLength, '0');

      const [emissionDateOffset, emissionDateLimit] = this.EMISSION_DATE_RANGE;
      const emissionDateNibbleLength = (emissionDateLimit - emissionDateOffset) * 2;

      const emissionDate = Math.floor(
        ((data.emissionDate ?? new Date().getTime()) - Pos.Calypso.ZeroTimeMillis) / (60 * 1000),
      )
        .toString(16)
        .padStart(emissionDateNibbleLength, '0');

      const newRecord =
        dataFormat +
        agencyCode +
        userCode +
        userProfile +
        profileDuration +
        cardStatus +
        emissionDate +
        taxCode +
        cardCircuit;

      return new this(byteArrayFromHex(newRecord));
    }

    private get recordParsed() {
      return {
        dataFormat: getBytes.apply(null, [this.recordHex, ...Record.DATA_FORMAT_RANGE]),
        agencyCode: getBytes.apply(null, [this.recordHex, ...Record.AGENCY_CODE_RANGE]),
        userCode: getBytes.apply(null, [this.recordHex, ...Record.USER_CODE_RANGE]),
        userProfile: getBytes.apply(null, [this.recordHex, ...Record.USER_PROFILE_RANGE]),
        profileDuration: getBytes.apply(null, [
          this.recordHex,
          ...EFEnvironment.Record.PROFILE_DURATION_RANGE,
        ]),
        cardStatus: getBytes.apply(null, [this.recordHex, ...Record.CARD_STATUS_RANGE]),
        emissionDate: getBytes.apply(null, [this.recordHex, ...Record.EMISSION_DATE_RANGE]),
        taxCode: getBytes.apply(null, [this.recordHex, ...Record.TAX_CODE_RANGE]),
        cardCircuit: getBytes.apply(null, [this.recordHex, ...Record.CARD_CIRCUIT_RANGE]),
      };
    }

    public get agencyCode() {
      return parseInt(this.recordParsed.agencyCode, 16);
    }

    public get userCode() {
      return parseInt(this.recordParsed.userCode, 16);
    }

    public get expirationInMonths() {
      return parseInt(this.recordParsed.profileDuration, 16);
    }

    public get emissionDate() {
      return parseInt(this.recordParsed.emissionDate, 16) * 60 * 1000 + Pos.Calypso.ZeroTimeMillis;
    }

    public get isExpired() {
      if (this.expirationInMonths === 0) {
        return false;
      }
      if (isFuture(this.emissionDate)) {
        return true;
      }
      const expirationDate = add(new Date(this.emissionDate), {
        months: this.expirationInMonths,
      });
      return isPast(expirationDate);
    }

    public get taxCode() {
      return this.recordParsed.taxCode
        .match(/../g)!
        .map((e) => String.fromCharCode(parseInt(e, 16)))
        .join('')
        .toUpperCase();
    }

    public get circuitIsBIP() {
      return (
        (this.recordParsed.cardCircuit as EFEnvironment.CardCircuit) ===
        EFEnvironment.CardCircuit.BIP
      );
    }

    public get dataFormatIsBIP() {
      return [
        EFEnvironment.CardDataFormat.BIPv2_2,
        EFEnvironment.CardDataFormat.BIPv2_3,
        EFEnvironment.CardDataFormat.BIPv2_4,
        EFEnvironment.CardDataFormat.BIPv2_5,
        EFEnvironment.CardDataFormat.BIPv3_0,
        EFEnvironment.CardDataFormat.BIPv3_1,
      ]
        .map((e) => (this.recordParsed.dataFormat as EFEnvironment.CardDataFormat) === e)
        .some(Boolean);
    }

    public get status() {
      return this.recordParsed.cardStatus as EFEnvironment.CardStatus;
    }
  }

  export enum CardCircuit {
    BIP = 'c0',
    PYOU = 'c1',
    EDISU = 'c2',
    NFC = 'c3',
    TRENITALIA = 'c4',
    CB = 'c5',
  }

  export enum CardDataFormat {
    GTTv1 = '00',
    GTTv2 = '01',
    BIPv2_2 = '02',
    BIPv2_3 = '03',
    BIPv2_4 = '04',
    BIPv2_5 = '05',
    BIPv3_0 = '05',
    BIPv3_1 = '05',
  }

  export enum CardStatus {
    Empty = '0',
    PrePersonalized = '1',
    Personalized = '2',
  }
}
