import * as React from 'react';
import { useEffect, useState } from 'react';

import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { PosPrinter, PosSam } from 'react-native-pos';

import data from './data.json';
import { format } from 'date-fns';
import { EFEnvironmentRecord } from './BIP/EFEnvironmentRecord';

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

  const environmentRecord = EFEnvironmentRecord.fromData({
    dataFormat: EFEnvironmentRecord.CardDataFormat.BIPv3_1,
    agencyCode: '05',
    userCode: user.id,
    userProfile,
    cardStatus: EFEnvironmentRecord.CardStatus.Personalized,
    taxCode: data.fiscal_code.toUpperCase(),
    cardCircuit: EFEnvironmentRecord.CardCircuit.BIP,
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
      PosSam.init().then(() => {
        setIsInitialized(true);
      });
    }

    const cardStatusListener = PosSam.addCardStatusListener((event) => {
      console.log(event.status);
    });

    return () => {
      cardStatusListener?.remove();
      PosSam.close();
    };
  }, [isInitialized]);

  const printHTML = async () => {
    setIsLoadingPrint(true);
    const isOpen = await PosPrinter.open();
    if (isOpen) {
      await PosPrinter.printHTML(
        '<h4 style="text-align:center;margin-top:10px;margin-bottom:0;">Durata</h4>'
      );
      await PosPrinter.close();
    }
    setIsLoadingPrint(false);
  };

  const printString = async () => {
    setIsLoadingPrint(true);
    const isOpen = await PosPrinter.open();
    if (isOpen) {
      PosPrinter.setGrey(6);
      PosPrinter.setLineSpace(5);
      PosPrinter.setBold(true);
      PosPrinter.setAlgin(PosPrinter.Mode.ALGIN_MIDDLE);
      PosPrinter.addString('Ciao Mondo');
      PosPrinter.printString();
      await PosPrinter.close();
    }
    setIsLoadingPrint(false);
  };

  const write = async () => {
    setIsLoadingWrite(true);
    try {
      const environmentRecord = dataToEnvironmentRecord(data);

      console.warn({
        emissionDate: format(
          new Date(environmentRecord.emissionDate),
          'dd/MM/yyyy HH:mm'
        ),
        isExpired: environmentRecord.isExpired,
        taxCode: environmentRecord.taxCode,
        circuitIsBIP: environmentRecord.circuitIsBIP,
        expirationInMonths: environmentRecord.expirationInMonths,
        dataFormatIsBIP: environmentRecord.dataFormatIsBIP,
      });

      await PosSam.writeToCardUpdate(environmentRecord.record, {
        application: EFEnvironmentRecord.AID,
        sfi: EFEnvironmentRecord.SFI,
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
      const cardId = await PosSam.readCardId();
      console.warn(cardId);
    } catch (e) {
      console.warn(e);
    }
    setIsLoadingRead(false);
  };

  const read = async () => {
    setIsLoadingRead(true);
    try {
      const { records, cardId } = await PosSam.readRecordsFromCard({
        application: EFEnvironmentRecord.AID,
        sfi: EFEnvironmentRecord.SFI,
        offset: 1,
        readMode: 1,
      });
      for (const key in records) {
        if (Object.prototype.hasOwnProperty.call(records, key)) {
          const record = records[key]!;

          const environmentRecord = new EFEnvironmentRecord(record);

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
          });
          console.warn(environmentRecord.record);
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
      <Pressable
        style={styles.button}
        onPress={printHTML}
        disabled={isLoadingPrint}
      >
        {isLoadingPrint ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>PRINT HTML</Text>
        )}
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
