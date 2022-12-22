import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-telpo' doesn't seem to be linked. Make sure: \n\n` +
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

export async function open(): Promise<boolean> {
  return await PosSam.open();
}

export async function close(): Promise<boolean> {
  return await PosSam.close();
}

export async function getManufacturer(): Promise<string> {
  return await PosSam.getManufacturer();
}

export async function transmit(command: number[]): Promise<number[]> {
  return await PosSam.transmit(command);
}

export async function challenge(): Promise<number[]> {
  return await PosSam.challenge();
}
