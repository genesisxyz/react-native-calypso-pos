import { NativeModules, Platform } from 'react-native';
import { ByteUtils } from './index';

const LINKING_ERROR =
  `The package 'react-native-pos' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const PosSam = NativeModules.PosSam
  ? NativeModules.PosSam
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

type CalypsoConstants = {
  ZERO_TIME_MILLIS: number;
  SAM_CHALLENGE_LENGTH_BYTES: number;
  AID: Uint8Array;
  EF_ENVIRONMENT_CARD_STATUS_INDEX: number;
  EF_ENVIRONMENT_CARD_STATUS_MASK: number;
  EF_ENVIRONMENT_EXPIRATION_MASK: Uint8Array;
  EF_ENVIRONMENT_EXPIRATION_INDEX: number;
  EF_ENVIRONMENT_EXPIRATION_LENGTH: number;
  EF_ENVIRONMENT_ISSUE_DATE_INDEX: number;
  EF_ENVIRONMENT_ISSUE_DATE_LENGTH: number;
  EF_ENVIRONMENT_DATA_FORMAT_INDEX: number;
  EF_ENVIRONMENT_CARD_CIRCUIT_INDEX: number;
  CARD_DATA_FORMAT: number;
  CARD_BIP_CIRCUIT: number;
  EF_ENVIRONMENT_TAX_CODE_INDEX: number;
  EF_ENVIRONMENT_TAX_CODE_LENGTH: number;
};

const constants = PosSam.getConstants();

export const CALYPSO: CalypsoConstants = {
  ...constants,
  AID: new Uint8Array(constants.AID),
  EF_ENVIRONMENT_EXPIRATION_MASK: new Uint8Array(
    constants.EF_ENVIRONMENT_EXPIRATION_MASK
  ),
};

export async function init(): Promise<boolean> {
  return await PosSam.init();
}

export async function readRecordsFromCard(): Promise<Record<number, number[]>> {
  return await PosSam.readRecordsFromCard();
}

export async function writeToCard(adpu: number[]): Promise<number[]> {
  return await PosSam.writeToCard(adpu);
}

function bitwiseAnd(array1: Uint8Array, array2: Uint8Array) {
  if (array1.length !== array2.length) return null;
  return array1.map((e, i) => {
    // @ts-ignore
    return e & array2[i];
  });
}

export enum CardStatus {
  Personalized,
  PrePersonalized,
  Unknown,
}

export class Card {
  record: Uint8Array;
  issueDateBytes: Uint8Array;

  constructor(record: Uint8Array) {
    this.record = record;
    this.issueDateBytes = record.slice(
      CALYPSO.EF_ENVIRONMENT_ISSUE_DATE_INDEX,
      CALYPSO.EF_ENVIRONMENT_ISSUE_DATE_INDEX +
        CALYPSO.EF_ENVIRONMENT_ISSUE_DATE_LENGTH
    );
  }

  cardStatus(): CardStatus {
    const status = this.record[CALYPSO.EF_ENVIRONMENT_CARD_STATUS_INDEX];
    if (!status) {
      return CardStatus.Unknown;
    }
    const cardStatus = status & CALYPSO.EF_ENVIRONMENT_CARD_STATUS_MASK;

    if (cardStatus === 0x01 || cardStatus === 0x00) {
      return CardStatus.PrePersonalized;
    } else if (cardStatus !== 0x02) {
      return CardStatus.Unknown;
    }

    return CardStatus.Personalized;
  }

  async isExpired(): Promise<boolean | null> {
    let expirationRaw = bitwiseAnd(
      CALYPSO.EF_ENVIRONMENT_EXPIRATION_MASK,
      this.record.slice(
        CALYPSO.EF_ENVIRONMENT_EXPIRATION_INDEX,
        CALYPSO.EF_ENVIRONMENT_EXPIRATION_INDEX +
          CALYPSO.EF_ENVIRONMENT_EXPIRATION_LENGTH
      )
    );

    if (!expirationRaw) return null;

    expirationRaw = await ByteUtils.shiftRight(expirationRaw, 4);

    if (!expirationRaw) return null;

    // byte expirationMonthsBytes = expirationRaw.length == 1 ? expirationRaw[0] : expirationRaw[1];
    const expirationMonthsBytes =
      expirationRaw.length === 1 ? expirationRaw[0] : expirationRaw[1];

    if (!expirationMonthsBytes) return null;

    // expiration equals to 0x00 indicates no expiration
    if (expirationMonthsBytes !== 0x00) {
      if (this.issueDateBytes.length < 3) return null;

      const issueDate =
        // @ts-ignore
        ((this.issueDateBytes[0] & 0xff) << 16) |
        // @ts-ignore
        ((this.issueDateBytes[1] & 0xff) << 8) |
        // @ts-ignore
        (this.issueDateBytes[2] & 0xff);

      // converting issue date to millis
      const issueDateMillisFromZeroTime = issueDate * 60 * 1000;

      const now = new Date();

      const actualIssueDateMillis =
        issueDateMillisFromZeroTime + CALYPSO.ZERO_TIME_MILLIS;

      if (now.getTime() < actualIssueDateMillis) {
        return true;
      }

      const expiration = new Date(actualIssueDateMillis);
      expiration.setMonth(expiration.getMonth() + expirationMonthsBytes);
      const yearsToAdd = Math.floor(expirationMonthsBytes / 12);
      expiration.setFullYear(expiration.getFullYear() + yearsToAdd);

      if (now.getTime() > expiration.getTime()) {
        return true;
      }
    }
    return false;
  }

  isDataFormatValid(): boolean {
    return (
      this.record[CALYPSO.EF_ENVIRONMENT_DATA_FORMAT_INDEX] ===
      CALYPSO.CARD_DATA_FORMAT
    );
  }

  isBIPCircuit(): boolean {
    return (
      this.record[CALYPSO.EF_ENVIRONMENT_CARD_CIRCUIT_INDEX] ===
      CALYPSO.CARD_BIP_CIRCUIT
    );
  }

  async taxCode(): Promise<string> {
    const taxCodeBytes = this.record.slice(
      CALYPSO.EF_ENVIRONMENT_TAX_CODE_INDEX,
      CALYPSO.EF_ENVIRONMENT_TAX_CODE_INDEX +
        CALYPSO.EF_ENVIRONMENT_TAX_CODE_LENGTH
    );

    return await ByteUtils.stringFromByteArray(taxCodeBytes);
  }
}
