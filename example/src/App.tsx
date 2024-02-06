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

import data from './data.json';
import { format } from 'date-fns';
import { EFEnvironment } from './BIP/EFEnvironmentRecord';
import { EFContractList } from './BIP/EFContractList';
import { EFContract } from './BIP/EFContract';

enum Profile {
  Ordinary = '700',
  Over65 = '702',
  Under25 = '700',
  Student = '701',
}

function dataToEnvironmentRecord(user: typeof data) {
  let userProfile = Profile.Ordinary;

  if (user.profile_type) {
    switch (user.profile_type) {
      case '65':
        userProfile = Profile.Over65;
        break;
      case '25':
        userProfile = Profile.Under25;
        break;
      case 'ST':
        userProfile = Profile.Student;
        break;
    }
  }

  const environmentRecord = EFEnvironment.Record.fromData({
    dataFormat: EFEnvironment.CardDataFormat.BIPv3_1,
    agencyCode: '05',
    userCode: user.id,
    userProfile,
    cardStatus: EFEnvironment.CardStatus.Personalized,
    taxCode: data.fiscal_code.toUpperCase(),
    cardCircuit: EFEnvironment.CardCircuit.BIP,
  });

  return environmentRecord;
}

export default function App() {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isLoadingPrint, setIsLoadingPrint] = useState(false);
  const [isLoadingWrite, setIsLoadingWrite] = useState(false);
  const [isLoadingRead, setIsLoadingRead] = useState(false);

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
    setIsLoadingWrite(true);
    try {
      const environmentRecord = dataToEnvironmentRecord(data);

      await Pos.writeToCardUpdate(environmentRecord.record, {
        application: EFEnvironment.Record.AID,
        sfi: EFEnvironment.Record.SFI,
        offset: 1,
        samUnlockString: '62 EE D0 33 FB 9F D1 85 B3 C7 DA BD 02 82 D6 EC',
      });
    } catch (e) {
      console.warn(e);
    }
    setIsLoadingWrite(false);
  };

  const readId = async () => {
    setIsLoadingRead(true);
    try {
      const cardId = await Pos.readCardId();
      console.warn(cardId);
    } catch (e) {
      console.warn(e);
    }
    setIsLoadingRead(false);
  };

  const read = async () => {
    setIsLoadingRead(true);
    try {
      const result = await Pos.readRecordsFromCard([
        {
          application: EFContractList.Record.AID,
          sfi: EFContractList.Record.SFI,
          offset: 1,
          readMode: Pos.ReadMode.OneRecord,
        },
        {
          application: EFContract.Record.AID,
          sfi: EFContract.Record.SFI,
          offset: 1,
          readMode: Pos.ReadMode.MultipleRecords,
        }
      ]);

      for (const e of result) {
        let i = 0;
        for (const key in e.records) {
          if (Object.prototype.hasOwnProperty.call(e.records, key)) {
            const record = e.records[key]!;

            console.warn(record, ++i);

            // const environmentRecord = new EFEnvironment.Record(record);

            /*
            console.warn({
              cardId,
              emissionDate: format(
                new Date(environmentRecord.emissionDate),
                'dd/MM/yyyy HH:mm'
              ),
              isExpired: environmentRecord.isExpired,
              taxCode: environmentRecord.taxCode,
              circuitIsBIP: environmentRecord.circuitIsBIP,
              expirationInMonths: environmentRecord.expirationInMonths,
              dataFormatIsBIP: environmentRecord.dataFormatIsBIP,
            });*/
          }
        }
      }

    } catch (e) {
      console.warn(e);
    }
    setIsLoadingRead(false);
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
        disabled={isLoadingRead}
      >
        {isLoadingRead ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>Read card ID</Text>
        )}
      </Pressable>
      <Pressable style={styles.button} onPress={read} disabled={isLoadingRead}>
        {isLoadingRead ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>Read card</Text>
        )}
      </Pressable>
      <Pressable
        style={styles.button}
        onPress={write}
        disabled={isLoadingWrite}
      >
        {isLoadingWrite ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>Write card</Text>
        )}
      </Pressable>
    </View>
  );
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
