import * as React from 'react';
import { useEffect, useState } from 'react';

import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Printer, Pos } from 'react-native-pos';

const SAM_UNLOCK_STRING = '';
const AID: Uint8Array = new Uint8Array(); // byteArrayFromHex('');
const SFI = 0x07;
const OFFSET = 1;

const RECORD_DATA = byteArrayFromHex(
  '000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c'
);

const EMPTY_DATA = byteArrayFromHex(
  '0000000000000000000000000000000000000000000000000000000000'
);

export default function App() {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isLoadingPrint, setIsLoadingPrint] = useState(false);
  const [isLoadingCard, setIsLoadingCard] = useState(false);

  if (SAM_UNLOCK_STRING.length === 0) {
    console.warn(
      'Please set SAM_UNLOCK_STRING as a hex string grouped by 2 characters'
    );
    return null;
  }

  if (AID.length === 0) {
    console.warn(
      "Please set AID as a hex string; e.g. byteArrayFromHex('abcdef');"
    );
    return null;
  }

  // eslint-disable-next-line react-hooks/rules-of-hooks
  useEffect(() => {
    if (!isInitialized) {
      Pos.init()
        .then(() => {
          setIsInitialized(true);
        })
        .catch((e) => {
          if (Pos.isPosError(e)) {
            if (e.code === Pos.ErrorCode.SamConnectFail) {
              console.warn('SAM connect fail');
            }
          }
        });
    }

    const cardStatusListener = Pos.addCardStatusListener((event) => {
      console.log(event.status);
    });

    return () => {
      cardStatusListener?.remove();
      if (isInitialized) {
        Pos.close();
      }
    };
  }, [isInitialized]);

  const debugPrint = async () => {
    const html = await Printer.printHtml([
      {
        type: 'text',
        data: 'Hello, World!',
        options: {
          align: Printer.Align.Center,
          size: 16,
          fontWeight: Printer.FontWeight.Bold,
        },
      },
    ]);

    console.log(html);
  };

  const printString = async () => {
    setIsLoadingPrint(true);
    const isOpen = await Printer.open();
    if (isOpen) {
      try {
        await Printer.print([
          {
            type: 'text',
            data: 'Hello, World!',
            options: {
              align: Printer.Align.Center,
              size: 16,
              fontWeight: Printer.FontWeight.Bold,
            },
          },
        ]);
        await Printer.close();
      } catch (e) {
        if (Printer.isPrinterError(e)) {
          if (e.code === Printer.ErrorCode.NoPaper) {
            console.warn('No paper');
          }
        }
      }
    }
    setIsLoadingPrint(false);
  };

  const write = async () => {
    if (SAM_UNLOCK_STRING.length === 0) {
      console.warn('Please set SAM_UNLOCK_STRING');
      return;
    }
    setIsLoadingCard(true);
    try {
      await Pos.writeToCardUpdate([
        {
          apdu: EMPTY_DATA,
          application: AID,
          sfi: SFI,
          offset: OFFSET,
          samUnlockString: SAM_UNLOCK_STRING,
        },
      ]);

      console.warn(`Wrote ${RECORD_DATA} to file ${SFI} on offset ${OFFSET}`);
    } catch (e) {
      console.warn(e);
    }
    setIsLoadingCard(false);
  };

  const readId = async () => {
    setIsLoadingCard(true);
    try {
      const cardId = await Pos.readCardId();
      console.warn(cardId);
    } catch (e) {
      console.warn(e);
    }
    setIsLoadingCard(false);
  };

  const read = async () => {
    setIsLoadingCard(true);
    try {
      const result = await Pos.readRecordsFromCard([
        {
          application: AID,
          sfi: SFI,
          offset: 1,
          readMode: Pos.ReadMode.OneRecord,
        },
      ]);

      console.log(JSON.stringify(result));
    } catch (e) {
      console.warn(e);
    }
    setIsLoadingCard(false);
  };

  const readWriteTest = async () => {
    setIsLoadingCard(true);
    try {
      await Pos.withBlock(async () => {
        let record = await Pos.read([
          {
            application: AID,
            sfi: SFI,
            offset: 1,
            readMode: Pos.ReadMode.OneRecord,
          },
        ]);
        console.log('Old record:', JSON.stringify(record.data));
        await Pos.write([
          {
            apdu: RECORD_DATA,
            application: AID,
            sfi: SFI,
            offset: OFFSET,
            samUnlockString: SAM_UNLOCK_STRING,
          },
        ]);
        record = await Pos.read([
          {
            application: AID,
            sfi: SFI,
            offset: OFFSET,
            readMode: Pos.ReadMode.OneRecord,
          },
        ]);
        console.log('New record:', JSON.stringify(record.data));
      });
    } catch (e) {
      console.warn(e);
    }
    setIsLoadingCard(false);
  };

  if (!isInitialized) return null;

  return (
    <View style={styles.container}>
      <Pressable style={styles.button} onPress={debugPrint}>
        <Text style={styles.text}>DEBUG PRINT</Text>
      </Pressable>
      <Pressable
        style={styles.button}
        onPress={printString}
        disabled={isLoadingPrint}
      >
        {isLoadingPrint ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>PRINT STRING</Text>
        )}
      </Pressable>
      <Pressable
        style={styles.button}
        onPress={readId}
        disabled={isLoadingCard}
      >
        {isLoadingCard ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>Read card ID</Text>
        )}
      </Pressable>
      <Pressable style={styles.button} onPress={read} disabled={isLoadingCard}>
        {isLoadingCard ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>Read card</Text>
        )}
      </Pressable>
      <Pressable style={styles.button} onPress={write} disabled={isLoadingCard}>
        {isLoadingCard ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>Write card</Text>
        )}
      </Pressable>
      <Pressable
        style={styles.button}
        onPress={readWriteTest}
        disabled={isLoadingCard}
      >
        {isLoadingCard ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>Read/Write test</Text>
        )}
      </Pressable>
    </View>
  );
}

export function byteArrayFromHex(recordHex: string) {
  return new Uint8Array(recordHex.match(/../g)!.map((e) => parseInt(e, 16)));
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  button: {
    marginBottom: 20,
    backgroundColor: '#6200EE',
    padding: 16,
  },
  text: {
    color: '#fff',
  },
});
