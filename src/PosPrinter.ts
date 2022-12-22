import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-pos' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const PosPrinter = NativeModules.PosPrinter
  ? NativeModules.PosPrinter
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export enum Status {
  STATUS_OK = 0,
  STATUS_NO_PAPER,
  STATUS_OVER_HEAT,
  STATUS_OVER_FLOW,
  STATUS_UNKNOWN,
  STATUS_ERROR = 16,
}

export enum Mode {
  ALGIN_LEFT = 0,
  ALGIN_MIDDLE,
  ALGIN_RIGHT,
}

export async function checkStatus(): Promise<Status> {
  return await PosPrinter.checkStatus();
}

export async function open(): Promise<boolean> {
  return await PosPrinter.open();
}

export function setGrey(level: number) {
  PosPrinter.setGrey(level);
}

export function setLineSpace(lineSpace: number) {
  PosPrinter.setLineSpace(lineSpace);
}

export function setBold(isBold: boolean) {
  PosPrinter.setBold(isBold);
}

export function setAlgin(mode: Mode) {
  PosPrinter.setAlgin(mode);
}

export function setTextSize(size: number) {
  PosPrinter.setTextSize(size);
}

export function addString(content: string) {
  PosPrinter.addString(content);
}

export function printString() {
  PosPrinter.printString();
}

export function walkPaper(line: number) {
  PosPrinter.walkPaper(line);
}

export function printLogo(image: string, isBuffer: boolean = false) {
  PosPrinter.printLogo(image, isBuffer);
}

export function printHTML() {
  PosPrinter.printHTML;
}

export async function close(): Promise<boolean> {
  return await PosPrinter.close();
}
