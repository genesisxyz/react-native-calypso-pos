import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-pos' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const Printer = NativeModules.Printer
  ? NativeModules.Printer
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export enum ErrorCode {
  NoPaper = 'NO_PAPER',
  Overheat = 'OVERHEAT',
  Overflow = 'OVERFLOW',
  Unknown = 'UNKNOWN',
}

export enum Align {
  Left = 'left',
  Center = 'center',
  Right = 'right',
}

export enum FontWeight {
  Normal = 'normal',
  Bold = 'bold',
}

export type PrintAction =
  | {
      type: 'logo';
      data: string;
      options?: {
        align?: Align;
      };
    }
  | {
      type: 'newLine';
      data: number;
    }
  | {
      type: 'text';
      data: string;
      options: {
        size: number;
        fontWeight?: FontWeight;
        align: Align;
      };
    };

export type PrinterError = {
  code: ErrorCode;
  message: string;
};

export function isPrinterError(obj: any): obj is PrinterError {
  return (
    (obj as PrinterError).code !== undefined &&
    (obj as PrinterError).message !== undefined
  );
}

export async function print(printActions: PrintAction[]): Promise<boolean> {
  return await Printer.print(printActions);
}

export async function open(): Promise<boolean> {
  return await Printer.open();
}

export async function close(): Promise<boolean> {
  return await Printer.close();
}

export async function printHtml(printActions: PrintAction[]): Promise<string> {
  return await Printer.printHtml(printActions);
}
