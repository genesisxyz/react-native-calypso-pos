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
import { useState } from 'react';
import { ByteUtils, PosPrinter, PosSam } from 'react-native-pos';

import data from './data.json';
import { bytesFromString } from '../../src/ByteUtils';
import { CardRecord } from '../../src/PosSam';

function concatArray(array1: Uint8Array, array2: Uint8Array) {
  let concatenatedArray = new Uint8Array(array1.length + array2.length);
  concatenatedArray.set(array1, 0);
  concatenatedArray.set(array2, array1.length);
  return concatenatedArray;
}

function padArrayStart(arr: Uint8Array, len: number, padding: number) {
  return concatArray(new Uint8Array(len - arr.length), arr);
}

async function dataString() {
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
  expiration = expiration.replace('', ' ').trim();

  // String status = "2"; // personalized
  const status = '2';

  // preparing tax code to write
  // String taxCode = cardUserData.getTaxCode();
  const taxCode = data.fiscal_code;

  // byte[] taxCodeBytes = taxCode.getBytes(StandardCharsets.UTF_8);
  const taxCodeBytes = await ByteUtils.bytesFromString(taxCode);

  // preparing emission time
  // byte[] emissionTimeOnesComplement = lastIssueDateBytes;

  console.warn(userCode, taxCodeBytes);

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
}

export default function App() {
  const [isLoadingPrint, setIsLoadingPrint] = useState(false);
  const [isLoadingSam, setIsLoadingSam] = useState(false);
  const [isLoadingRead, setIsLoadingRead] = useState(false);

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

  const sam = async () => {
    setIsLoadingSam(true);
    /*
    const isInitialized = await PosSam.init();
    if (isInitialized) {
      const response = await PosSam.writeToCard([]);
      console.warn(response);
    }
    */
    await dataString();
    setIsLoadingSam(false);
  };

  const read = async () => {
    setIsLoadingRead(true);
    const isInitialized = await PosSam.init();
    // const isInitialized = true;
    if (isInitialized) {
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
    }
    setIsLoadingRead(false);
  };

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
      <Pressable style={styles.button} onPress={sam} disabled={isLoadingSam}>
        {isLoadingSam ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>Sam Challenge</Text>
        )}
      </Pressable>
      <Pressable style={styles.button} onPress={read} disabled={isLoadingRead}>
        {isLoadingRead ? (
          <ActivityIndicator />
        ) : (
          <Text style={styles.text}>Read card</Text>
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
