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

export async function init(): Promise<boolean> {
  return await PosSam.init();
}

export async function readRecordsFromCard(): Promise<Record<number, number[]>> {
  return await PosSam.readRecordsFromCard();
}

export async function writeToCard(adpu: number[]): Promise<number[]> {
  return await PosSam.writeToCard(adpu);
}
