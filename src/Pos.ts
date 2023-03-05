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

export async function init(): Promise<boolean> {
  return await Pos.init();
}

export function close() {
  return Pos.close();
}

export async function readCardId(): Promise<{ samId: string; cardId: string }> {
  return await Pos.readCardId();
}

export async function readRecordsFromCard(options: {
  application: Uint8Array;
  sfi: number;
  offset: number;
  readMode: ReadMode;
}): Promise<{
  records: Record<number, number[]>;
  cardId: string;
}> {
  return await Pos.readRecordsFromCard({
    ...options,
    application: Array.from(options.application),
  });
}

export async function writeToCardUpdate(
  adpu: Uint8Array,
  options: {
    application: Uint8Array;
    sfi: number;
    offset: number;
    samUnlockString: string;
  }
): Promise<void> {
  return await Pos.writeToCardUpdate(Array.from(adpu), {
    ...options,
    application: Array.from(options.application),
  });
}

export function addCardStatusListener(
  listener: (event: { status: 'detected' }) => void
) {
  if (Platform.OS === 'android') {
    const eventEmitter = new NativeEventEmitter();
    return eventEmitter.addListener('CardStatus', listener);
  }
  return null;
}
