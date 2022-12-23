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

  const sam = async () => {
    setIsLoadingSam(true);
    const isOpen = await PosSam.open();
    if (isOpen) {
      const command = await PosSam.challenge();
      console.warn(command.map((e) => (e & 0xff).toString(16)));
      const res = await PosSam.transmit(command);
      console.warn(res.map((e) => (e & 0xff).toString(16)));
      await PosSam.close();
    }
    setIsLoadingSam(false);
  };

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
