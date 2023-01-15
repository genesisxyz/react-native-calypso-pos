/* eslint-disable @typescript-eslint/no-unused-vars,no-bitwise */
// noinspection ES6UnusedImports
// @ts-nocheck

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
import { bytesFromString } from '../../src/ByteUtils';
import { CardRecord } from '../../src/PosSam';
import { format, parse } from 'date-fns';

function concatArray(array1: Uint8Array, array2: Uint8Array) {
  let concatenatedArray = new Uint8Array(array1.length + array2.length);
  concatenatedArray.set(array1, 0);
  concatenatedArray.set(array2, array1.length);
  return concatenatedArray;
}

function padArrayStart(arr: Uint8Array, len: number, padding: number) {
  return concatArray(new Uint8Array(len - arr.length), arr);
}

async function dataString(card: PosSam.Card) {
  /*
  byte[] tempUserCodeBytes = ByteConvertStringUtil.stringToByteArray(Long.toHexString(cardUserData.getId()));
  */
  const idToHex = Number(data.id).toString(16);
  const tempUserCodeBytes = await ByteUtils.stringToByteArray(idToHex);

  /*
  byte[] userCodeBytes = new byte[4];
  System.arraycopy(tempUserCodeBytes, 0, userCodeBytes, userCodeBytes.length - tempUserCodeBytes.length, tempUserCodeBytes.length);
  */
  const userCodeBytes = padArrayStart(tempUserCodeBytes, 4, 0);

  // String userCode = ByteConvertStringUtil.bytesToHexString(userCodeBytes);
  const userCode = await ByteUtils.bytesToHexString(userCodeBytes);

  const lastUserData = data;
  // String onlineUserProfile = lastUserData.getProfileType();
  const onlineUserProfile = lastUserData.profile_type;

  // String userProfile = BuildConfig.ORDINARY_PROFILE;
  let userProfile = '70 0';

  /*
  if (onlineUserProfile != null) {
      if(onlineUserProfile.equalsIgnoreCase("65"))
          userProfile = BuildConfig.OVER_65_PROFILE;
      else if(onlineUserProfile.equalsIgnoreCase("25"))
          userProfile = BuildConfig.UNDER_25_PROFILE;
      else if(onlineUserProfile.equalsIgnoreCase("ST"))
          userProfile = BuildConfig.STUDENT_PROFILE;
  }
  */
  if (onlineUserProfile) {
    switch (onlineUserProfile.toLowerCase()) {
      case '65':
        userProfile = '70 2';
        break;
      case '25':
        userProfile = '70 0';
        break;
      case 'ST':
        userProfile = '70 1';
        break;
    }
  }

  // String expiration = lastUserData.getExpirationMonths() == -1 ? "00" : String.format("%02x", lastUserData.getExpirationMonths());
  let expiration =
    lastUserData.expiration_months === -1
      ? '00'
      : Number(lastUserData.expiration_months).toString(16).padStart(2, '0');

  // expiration = expiration.replace("", " ").trim();
  expiration = expiration.split('').join(' ');

  // String status = "2"; // personalized
  const status = '2';

  // preparing tax code to write
  // String taxCode = cardUserData.getTaxCode();
  const taxCode = data.fiscal_code.toUpperCase();

  // byte[] taxCodeBytes = taxCode.getBytes(StandardCharsets.UTF_8);
  const taxCodeBytes = await ByteUtils.bytesFromString(taxCode);

  // preparing emission time
  // byte[] emissionTimeOnesComplement = lastIssueDateBytes;
  let emissionTimeOnesComplement = card.issueDateBytes;

  /*
  // if read card doesn't have an emission time, calculate it
  if (emissionTimeOnesComplement == null) {
      Calendar initializedTimeCalendar = cardUserData.getActualInitializedTime();
      if (initializedTimeCalendar == null) {
          cardUserData.setInitialized(false);

          CardCloseSessionBuilder cardCloseSessionBuilder =
                  new CardCloseSessionBuilder();
          ApduResponseAdapter cardCloseSessionResponseAdapter =
                  new ApduResponseAdapter(
                          transmitToCard(cardCloseSessionBuilder.getApdu()));
          CardCloseSessionParser cardCloseSessionParser = cardCloseSessionBuilder
                  .createResponseParser(cardCloseSessionResponseAdapter);

          cardCloseSessionParser.checkStatus();

          return;
      }
  */
  // TODO: why is the code above required?
  if (!emissionTimeOnesComplement) {
    // long emissionTimeMillis = cardUserData.getActualInitializedTime().getTimeInMillis();
    const emissionTimeMillis = parse(
      data.initialized_time,
      'yyyy-MM-dd HH:mm:ss',
      new Date()
    ).getTime();

    // long emissionTimeFromZeroTimeMillis = emissionTimeMillis - Calypso.ZERO_TIME_MILLIS;
    const emissionTimeFromZeroTimeMillis =
      emissionTimeMillis - PosSam.CALYPSO.ZERO_TIME_MILLIS;

    // int emissionTimeFromZeroTimeMinutes = (int) Math.floor(emissionTimeFromZeroTimeMillis / (60.0 * 1_000));
    const emissionTimeFromZeroTimeMinutes = Math.floor(
      emissionTimeFromZeroTimeMillis / (60 * 1000)
    );

    // using a temp value since its length could be different from
    // requested one
    // byte[] tempEmissionTimeFromZeroTimeMinutesBytes = new BigInteger(String.valueOf(emissionTimeFromZeroTimeMinutes), 10).toByteArray();
    const tempEmissionTimeFromZeroTimeMinutesBytes =
      await ByteUtils.bytesFromString(String(emissionTimeFromZeroTimeMinutes));

    // byte[] emissionTimeFromZeroTimeMinutesBytes = new byte[Calypso.CARD_EMISSION_TIME_LENGTH_IN_BYTES];
    let emissionTimeFromZeroTimeMinutesBytes = new Uint8Array([
      PosSam.CALYPSO.CARD_EMISSION_TIME_LENGTH_IN_BYTES,
    ]);

    // final int emissionTimeInBytesLength = tempEmissionTimeFromZeroTimeMinutesBytes.length;
    const emissionTimeInBytesLength =
      tempEmissionTimeFromZeroTimeMinutesBytes.length;

    // if length of emission time in bytes in greater than required one,
    // take only useful bytes
    /*
    if (emissionTimeInBytesLength == Calypso.CARD_EMISSION_TIME_LENGTH_IN_BYTES)
        System.arraycopy(tempEmissionTimeFromZeroTimeMinutesBytes, 0,
                emissionTimeFromZeroTimeMinutesBytes, 0,
                Calypso.CARD_EMISSION_TIME_LENGTH_IN_BYTES);
    else
        System.arraycopy(tempEmissionTimeFromZeroTimeMinutesBytes,
                emissionTimeInBytesLength - Calypso.CARD_EMISSION_TIME_LENGTH_IN_BYTES,
                emissionTimeFromZeroTimeMinutesBytes, 0,
                Calypso.CARD_EMISSION_TIME_LENGTH_IN_BYTES);
    */
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

    // emissionTimeOnesComplement = ByteUtils.onesComplement(emissionTimeFromZeroTimeMinutesBytes);
    // emissionTimeOnesComplement = emissionTimeFromZeroTimeMinutesBytes;
    emissionTimeOnesComplement = emissionTimeFromZeroTimeMinutesBytes;
  }

  // putting data to write together
  // StringBuilder newRecordDataBuilder = new StringBuilder();
  let newRecordDataBuilder = '';
  // newRecordDataBuilder.append(ByteConvertStringUtil.byteToHexString(Calypso.CARD_DATA_FORMAT)).append(" ");
  newRecordDataBuilder +=
    (await ByteUtils.byteToHexString(PosSam.CALYPSO.CARD_DATA_FORMAT)) + ' ';
  // newRecordDataBuilder.append(BuildConfig.bip_agency_code).append(" ");
  const bip_agency_code = '05';
  newRecordDataBuilder += bip_agency_code + ' ';
  // newRecordDataBuilder.append(userCode).append(" ");
  newRecordDataBuilder += userCode! + ' ';
  // newRecordDataBuilder.append(userProfile);
  newRecordDataBuilder += userProfile;
  // newRecordDataBuilder.append(expiration);
  newRecordDataBuilder += expiration;
  // newRecordDataBuilder.append(status).append(" ");
  newRecordDataBuilder += status + ' ';
  // newRecordDataBuilder.append(ByteConvertStringUtil.bytesToHexString(emissionTimeOnesComplement)).append(" ");
  newRecordDataBuilder +=
    (await ByteUtils.bytesToHexString(emissionTimeOnesComplement)) + ' ';
  // newRecordDataBuilder.append(ByteConvertStringUtil.bytesToHexString(taxCodeBytes)).append(" ");
  newRecordDataBuilder +=
    (await ByteUtils.bytesToHexString(taxCodeBytes)) + ' ';
  // newRecordDataBuilder.append(ByteConvertStringUtil.byteToHexString(Calypso.CARD_BIP_CIRCUIT));
  newRecordDataBuilder += await ByteUtils.byteToHexString(
    PosSam.CALYPSO.CARD_BIP_CIRCUIT
  );

  // String newRecordData = newRecordDataBuilder.toString();
  const newRecordData = newRecordDataBuilder;
  // int byteCount = newRecordData.split("\\s+").length;
  // converting data to write in bytes
  // byte[] newRecordDataBytes = new byte[byteCount];
  // ByteConvertStringUtil.stringToByteArray(newRecordDataBuilder.toString(), newRecordDataBytes);
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
      // const records = await PosSam.readRecordsFromCard();
      const records = {
        '1': [
          5, 5, 0, 0, 0, 85, 112, 39, -126, -112, 23, 115, 84, 83, 84, 84, 83,
          84, 53, 54, 68, 52, 54, 76, 50, 49, 57, 71, -64,
        ],
      };
      let record: PosSam.Card;
      for (const key in records) {
        record = new PosSam.Card(records[key]);

        if (
          record.cardStatus() !== PosSam.CardStatus.Unknown &&
          !(await record.isExpired()) &&
          record.isDataFormatValid() &&
          record.isBIPCircuit() &&
          data.initialized_time
        ) {
          const taxCode = await record.taxCode();
          if (taxCode.toLowerCase() !== data.fiscal_code) {
            // TODO: set the card as not initialized
          }
          const newRecord = await dataString(record);
          await PosSam.writeToCard(newRecord);
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
      /*
      const records = {
        '1': [
          5, 5, 0, 0, 0, 85, 112, 39, -126, -112, 23, 115, 84, 83, 84, 84, 83,
          84, 53, 54, 68, 52, 54, 76, 50, 49, 57, 71, -64,
        ],
      };
      */
      for (const key in records) {
        const record = new PosSam.Card(records[key]);

        if (
          record.cardStatus() !== PosSam.CardStatus.Unknown &&
          !(await record.isExpired()) &&
          record.isDataFormatValid() &&
          record.isBIPCircuit()
        ) {
          const taxCode = await record.taxCode();
          console.warn('TAX CODE', taxCode);
          if (taxCode.toLowerCase() !== data.fiscal_code) {
            // TODO: set the card as not initialized
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
