import * as React from 'react';

import {
  StyleSheet,
  View,
  Text,
  Pressable,
  ActivityIndicator,
} from 'react-native';
import { useState } from 'react';
import { PosPrinter, PosSam } from 'react-native-pos';

export default function App() {
  const [isLoadingPrint, setIsLoadingPrint] = useState(false);
  const [isLoadingSam, setIsLoadingSam] = useState(false);

  /*function padArrayStart<T = number>(arr: T[], len: number, padding: T) {
    return Array<T>(len - arr.length)
      .fill(padding)
      .concat(arr);
  }*/

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
    const isInitialized = await PosSam.init();
    if (isInitialized) {
      const response = await PosSam.writeToCard([]);
      console.warn(response);
    }
    setIsLoadingSam(false);
  };

  /*const utils = async () => {
    const idToHex = Number(data.id).toString(16);
    const tempUserCodeBytes = await ByteUtils.stringToByteArray(idToHex);
    const userCodeBytes = padArrayStart(tempUserCodeBytes, 4, 0);
    const userCode = await ByteUtils.bytesToHexString(userCodeBytes);
    const onlineUserProfile = data.profile_type;
    let userProfile = '70 0';
    if (onlineUserProfile != null) {
      if (onlineUserProfile.toLowerCase() === '65') userProfile = '70 2';
      else if (onlineUserProfile.toLowerCase() === '25') userProfile = '70 0';
      else if (onlineUserProfile.toLowerCase() === 'st') userProfile = '70 1';
    }
    let expiration =
      data.expiration_months === -1
        ? '00'
        : data.expiration_months.toString().padStart(2, '0');
    expiration = expiration.replace(/\s/g, ' ').trim();
    const status = '2';
    const taxCode = data.fiscal_code;
    const taxCodeBytes = await ByteUtils.bytesFromString(taxCode);
    const records = await PosSam.readRecordsFromCard();
    console.warn(records);
  };*/

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
