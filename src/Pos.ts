import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-pos' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const Pos = NativeModules.Pos
  ? NativeModules.Pos
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

type CalypsoConstants = {
  ZeroTimeMillis: number; // TODO: THIS IS NOT CALYPSO THIS IS DEFINED ON BIP
};

const constants = Pos.getConstants();

export const Calypso: CalypsoConstants = constants;

export enum CardStatus {
  Unknown,
  PrePersonalized,
  Personalized,
}

export enum ReadMode {
  OneRecord,
  MultipleRecords,
}

export type PosError = {
  code: ErrorCode;
  message: string;
};

export enum ErrorCode {
  Unknown = 'UNKNOWN',
  CardNotSupported = 'CARD_NOT_SUPPORTED',
  CardNotPresent = 'CARD_NOT_PRESENT',
  CardNotConnected = 'CARD_NOT_CONNECTED',
  CardConnectFail = 'CARD_CONNECT_FAIL',
  CardDisconnectFail = 'CARD_DISCONNECT_FAIL',
  TransmitApduCommand = 'TRANSMIT_APDU_COMMAND',
  PendingRequest = 'PENDING_REQUEST',
  SamConnectFail = 'SAM_CONNECT_FAIL',
  SamDisconnectFail = 'SAM_DISCONNECT_FAIL',
}

export function isPosError(obj: any): obj is PosError {
  return (
    (obj as PosError).code !== undefined &&
    (obj as PosError).message !== undefined
  );
}

export async function init(): Promise<boolean> {
  return await Pos.init();
}

export function close() {
  return Pos.close();
}

export async function readCardId(): Promise<{ samId: string; cardId: string }> {
  return await Pos.readCardId();
}

export async function readRecordsFromCard(
  options: {
    application: Uint8Array;
    sfi: number;
    offset: number;
    readMode: ReadMode;
  }[]
): Promise<{
  cardId: string;
  samId: string | null;
  data: {
    sfi: number;
    records: Record<number, number[]>;
  }[];
}> {
  return await Pos.readRecordsFromCard(
    options.map((e) => ({
      ...e,
      application: Array.from(e.application),
    }))
  );
}

export async function writeToCardUpdate(
  options: {
    apdu: Uint8Array;
    application: Uint8Array;
    sfi: number;
    offset: number;
    samUnlockString: string;
  }[]
): Promise<void> {
  return await Pos.writeToCardUpdate(
    options.map((e) => ({
      ...e,
      apdu: Array.from(e.apdu),
      application: Array.from(e.application),
    }))
  );
}

export function addCardStatusListener(
  listener: (event: { status: 'detected' }) => void
) {
  if (Platform.OS === 'ios') {
    const eventEmitter = new NativeEventEmitter(NativeModules.MyEventEmitter);
    return eventEmitter.addListener('CardStatus', listener);
  }
  const eventEmitter = new NativeEventEmitter();
  return eventEmitter.addListener('CardStatus', listener);
}
