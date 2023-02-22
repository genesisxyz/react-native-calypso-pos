import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

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
  ZERO_TIME_MILLIS: number; // TODO: THIS IS NOT CALYPSO THIS IS DEFINED ON BIP
};

const constants = PosSam.getConstants();

export const CALYPSO: CalypsoConstants = constants;

export async function init(): Promise<boolean> {
  return await PosSam.init();
}

export function close() {
  return PosSam.close();
}

export async function readCardId(): Promise<{ samId: string; cardId: string }> {
  return await PosSam.readCardId();
}

export async function readRecordsFromCard(options: {
  application: Uint8Array;
  sfi: number;
  offset: number;
  readMode: number;
}): Promise<{
  records: Record<number, number[]>;
  cardId: string;
}> {
  return await PosSam.readRecordsFromCard({
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
  return await PosSam.writeToCardUpdate(Array.from(adpu), {
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

export enum CardStatus {
  Unknown,
  PrePersonalized,
  Personalized,
}
