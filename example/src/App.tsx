import * as React from 'react';

import {
  StyleSheet,
  View,
  Text,
  Pressable,
  ActivityIndicator,
} from 'react-native';
import { useEffect, useState } from 'react';
import { ByteUtils, PosPrinter, PosSam } from 'react-native-pos';

import data from './data.json';
import { parse } from 'date-fns';

enum Profile {
  Ordinary = '70 0',
  Over65 = '70 2',
  Under25 = '70 0',
  Student = '70 1',
}

async function dataString(card: PosSam.Card) {
  const idToHex = Number(data.id).toString(16);
  const tempUserCodeBytes = await ByteUtils.stringToByteArray(idToHex);

  if (!tempUserCodeBytes) return null;

  const userCodeBytes = ByteUtils.padArrayStart(tempUserCodeBytes, 4);

  const userCode = await ByteUtils.bytesToHexString(userCodeBytes);

  const lastUserData = data;
  const onlineUserProfile = lastUserData.profile_type;

  let userProfile = Profile.Ordinary;

  if (onlineUserProfile) {
    switch (onlineUserProfile.toLowerCase()) {
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

  let expiration =
    lastUserData.expiration_months === -1
      ? '00'
      : Number(lastUserData.expiration_months).toString(16).padStart(2, '0');

  expiration = expiration.split('').join(' ');

  const status = '2';

  // preparing tax code to write
  const taxCode = data.fiscal_code.toUpperCase();

  const taxCodeBytes = await ByteUtils.bytesFromString(taxCode);

  if (!taxCodeBytes) return null;

  // preparing emission time
  let emissionTimeOnesComplement = card.issueDateBytes;

  if (!emissionTimeOnesComplement) {
    const emissionTimeMillis = parse(
      data.initialized_time,
      'yyyy-MM-dd HH:mm:ss',
      new Date()
    ).getTime();

    const emissionTimeFromZeroTimeMillis =
      emissionTimeMillis - PosSam.CALYPSO.ZERO_TIME_MILLIS;

    const emissionTimeFromZeroTimeMinutes = Math.floor(
      emissionTimeFromZeroTimeMillis / (60 * 1000)
    );

    // using a temp value since its length could be different from requested one
    const tempEmissionTimeFromZeroTimeMinutesBytes =
      await ByteUtils.bytesFromString(String(emissionTimeFromZeroTimeMinutes));

    if (!tempEmissionTimeFromZeroTimeMinutesBytes) return null;

    let emissionTimeFromZeroTimeMinutesBytes = new Uint8Array([
      PosSam.CALYPSO.CARD_EMISSION_TIME_LENGTH_IN_BYTES,
    ]);

    const emissionTimeInBytesLength =
      tempEmissionTimeFromZeroTimeMinutesBytes.length;

    // if length of emission time in bytes in greater than required one,
    // take only useful bytes
    if (
      emissionTimeInBytesLength ===
      PosSam.CALYPSO.CARD_EMISSION_TIME_LENGTH_IN_BYTES
    ) {
      emissionTimeFromZeroTimeMinutesBytes =
        tempEmissionTimeFromZeroTimeMinutesBytes.slice(
          0,
          PosSam.CALYPSO.CARD_EMISSION_TIME_LENGTH_IN_BYTES
        );
    } else {
      emissionTimeFromZeroTimeMinutesBytes =
        tempEmissionTimeFromZeroTimeMinutesBytes.slice(
          emissionTimeInBytesLength -
            PosSam.CALYPSO.CARD_EMISSION_TIME_LENGTH_IN_BYTES,
          PosSam.CALYPSO.CARD_EMISSION_TIME_LENGTH_IN_BYTES
        );
    }

    emissionTimeOnesComplement = emissionTimeFromZeroTimeMinutesBytes;
  }

  // putting data to write together
  let newRecordDataBuilder = '';
  newRecordDataBuilder +=
    (await ByteUtils.byteToHexString(PosSam.CALYPSO.CARD_DATA_FORMAT)) + ' ';
  const bip_agency_code = '05';
  newRecordDataBuilder += bip_agency_code + ' ';
  newRecordDataBuilder += userCode! + ' ';
  newRecordDataBuilder += userProfile;
  newRecordDataBuilder += expiration;
  newRecordDataBuilder += status + ' ';
  newRecordDataBuilder +=
    (await ByteUtils.bytesToHexString(emissionTimeOnesComplement)) + ' ';
  newRecordDataBuilder +=
    (await ByteUtils.bytesToHexString(taxCodeBytes)) + ' ';
  newRecordDataBuilder += await ByteUtils.byteToHexString(
    PosSam.CALYPSO.CARD_BIP_CIRCUIT
  );

  // converting data to write in bytes
  const newRecordDataBytes = await ByteUtils.makeByteArrayFromString(
    newRecordDataBuilder
  );

  return newRecordDataBytes;
}

export default function App() {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isLoadingPrint, setIsLoadingPrint] = useState(false);
  const [isLoadingWrite, setIsLoadingWrite] = useState(false);
  const [isLoadingRead, setIsLoadingRead] = useState(false);

  useEffect(() => {
    PosSam.init().then(() => {
      setIsInitialized(true);
    });
  }, []);

  const print = async () => {
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
      const records = await PosSam.readRecordsFromCard();
      /*
      const records = {
        '1': [
          5, 5, 0, 0, 0, 85, 112, 39, -126, -112, 23, 115, 84, 83, 84, 84, 83,
          84, 53, 54, 68, 52, 54, 76, 50, 49, 57, 71, -64,
        ],
      };
      */
      let record: PosSam.Card;
      for (const key in records) {
        record = new PosSam.Card(records[key]!);

        if (
          record.cardStatus() !== PosSam.CardStatus.Unknown &&
          !(await record.isExpired()) &&
          record.isDataFormatValid() &&
          record.isBIPCircuit() &&
          data.initialized_time
        ) {
          const newRecord = await dataString(record);
          if (newRecord) {
            await PosSam.writeToCard(newRecord);
          }
        }
      }
    } catch (e) {
      console.warn(e);
    }
    setIsLoadingWrite(false);
  };

  const read = async () => {
    setIsLoadingRead(true);
    try {
      const records = await PosSam.readRecordsFromCard();
      for (const key in records) {
        const record = new PosSam.Card(records[key]!);

        if (
          record.cardStatus() !== PosSam.CardStatus.Unknown &&
          !(await record.isExpired()) &&
          record.isDataFormatValid() &&
          record.isBIPCircuit()
        ) {
          const taxCode = await record.taxCode();
          console.warn('TAX CODE', taxCode);
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
        onPress={print}
        disabled={isLoadingPrint}
      >
        {isLoadingPrint ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>PRINT</Text>
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
