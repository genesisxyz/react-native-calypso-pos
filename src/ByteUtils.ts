import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-pos' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const ByteUtils = NativeModules.ByteUtils
  ? NativeModules.ByteUtils
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export async function stringToByteArray(str: string): Promise<number[]> {
  return await ByteUtils.stringToByteArray(str);
}

export async function bytesFromString(str: string): Promise<number[]> {
  return await ByteUtils.bytesFromString(str);
}

export async function bytesToHexString(bytes: number[]): Promise<string> {
  return await ByteUtils.bytesToHexString(bytes);
}
