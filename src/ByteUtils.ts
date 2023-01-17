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

export async function stringToByteArray(
  str: string
): Promise<Uint8Array | null> {
  const bytes = await ByteUtils.stringToByteArray(str);
  return bytes ? new Uint8Array(bytes) : null;
}

export async function makeByteArrayFromString(
  str: string
): Promise<Uint8Array> {
  const bytes = await ByteUtils.makeByteArrayFromString(str);
  return new Uint8Array(bytes);
}

export async function bytesFromString(str: string): Promise<Uint8Array | null> {
  const bytes = await ByteUtils.bytesFromString(str);
  return bytes ? new Uint8Array(bytes) : null;
}

export async function bytesToHexString(
  bytes: Uint8Array
): Promise<string | null> {
  return await ByteUtils.bytesToHexString(Array.from(bytes));
}

export async function byteToHexString(byte: number): Promise<string> {
  return await ByteUtils.byteToHexString(byte);
}

export async function shiftRight(
  bytes: Uint8Array,
  n: number
): Promise<Uint8Array> {
  return new Uint8Array(await ByteUtils.shiftRight(Array.from(bytes), n));
}

export async function stringFromByteArray(bytes: Uint8Array): Promise<string> {
  return await ByteUtils.stringFromByteArray(bytes);
}

export function concatArray(array1: Uint8Array, array2: Uint8Array) {
  let concatenatedArray = new Uint8Array(array1.length + array2.length);
  concatenatedArray.set(array1, 0);
  concatenatedArray.set(array2, array1.length);
  return concatenatedArray;
}

export function padArrayStart(arr: Uint8Array, len: number) {
  return concatArray(new Uint8Array(len - arr.length), arr);
}
