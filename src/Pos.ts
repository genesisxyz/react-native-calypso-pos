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
  userInfo: {
    isPosError: true;
  };
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
  NoSamAvailable = 'NO_SAM_AVAILABLE',
  Cancel = 'CANCEL',
}

export function isPosError(obj: any): obj is PosError {
  return (
    (obj as PosError).userInfo?.isPosError === true &&
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

export async function getSamId(): Promise<{ samId: string | null }> {
  return await Pos.getSamId();
}

export async function read(
  options: {
    application: Uint8Array;
    sfi: number;
    offset: number;
    readMode: ReadMode;
  }[]
): Promise<
  {
    sfi: number;
    records: Record<number, number[]>;
  }[]
> {
  return await Pos.unsafeRead(
    options.map((e) => ({
      ...e,
      application: Array.from(e.application),
    }))
  );
}

export async function write(
  options: {
    apdu: Uint8Array;
    application: Uint8Array;
    sfi: number;
    offset?: number;
    samUnlockString: string;
  }[]
): Promise<void> {
  return await Pos.unsafeWrite(
    options.map((e) => ({
      ...e,
      apdu: Array.from(e.apdu),
      application: Array.from(e.application),
    }))
  );
}

export async function withBlock(
  block: (info: { cardId: string }) => Promise<void>
) {
  try {
    const { cardId } = (await Pos.unsafeWaitForCard()) as {
      cardId: string;
    };
    await Pos.unsafeConnectCard();
    await block({ cardId });
  } catch (error) {
    throw error;
  } finally {
    await Pos.unsafeDisconnectCard();
  }
}

export async function samComputeEventLogSignature(options: {
  samUnlockString: string;
  kif: number;
  kvc: number;
  log: Uint8Array;
}): Promise<Uint8Array> {
  const response = await Pos.samComputeEventLogSignature({
    ...options,
    log: Array.from(options.log),
  });
  return new Uint8Array(response);
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
