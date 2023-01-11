import { NativeModules, Platform } from 'react-native';

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
  AID: number[];
  EF_ENVIRONMENT_CARD_STATUS_INDEX: number;
  EF_ENVIRONMENT_CARD_STATUS_MASK: number;
  EF_ENVIRONMENT_EXPIRATION_MASK: number[];
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

export const CALYPSO: CalypsoConstants = PosSam.getConstants();

export async function init(): Promise<boolean> {
  return await PosSam.init();
}

export async function readRecordsFromCard(): Promise<Record<number, number[]>> {
  return await PosSam.readRecordsFromCard();
}

export async function writeToCard(adpu: number[]): Promise<number[]> {
  return await PosSam.writeToCard(adpu);
}
