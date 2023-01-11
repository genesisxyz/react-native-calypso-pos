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

function padArrayStart<T = number>(arr: T[], len: number, padding: T) {
  return Array<T>(len - arr.length)
    .fill(padding)
    .concat(arr);
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
  const taxCodeBytes = ByteUtils.bytesFromString(taxCode);

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

function bitwiseAnd(array1: number[], array2: number[]) {
  if (array1.length !== array2.length) return null;
  return array1.map((e, i) => {
    return e & array2[i];
  });
}

let lastIssueDateBytes: number[] = [];

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
          const record: number[] = records[key];

          // checking if card is personalized
          // byte cardStatus = (byte) (record[Calypso.EF_ENVIRONMENT_CARD_STATUS_INDEX] & Calypso.EF_ENVIRONMENT_CARD_STATUS_MASK);
          const cardStatus =
            record[PosSam.CALYPSO.EF_ENVIRONMENT_CARD_STATUS_INDEX] &
            PosSam.CALYPSO.EF_ENVIRONMENT_CARD_STATUS_MASK;

          /*
          if (cardStatus == 0x01 || cardStatus == 0x00) {
              // pre-personalized card, skip
              continue;
          } else if (cardStatus != 0x02)
              throw new IllegalStateException("Unknown card status");
          */
          if (cardStatus == 0x01 || cardStatus == 0x00) {
            // pre-personalized card, skip
          } else if (cardStatus != 0x02) {
            throw 'Unknown card status';
          }

          // personalized card, checking if card is not expired

          /*
          byte[] expirationRaw = ByteUtils.bitwiseAnd(
            Calypso.EF_ENVIRONMENT_EXPIRATION_MASK,
            Arrays.copyOfRange(record,
              Calypso.EF_ENVIRONMENT_EXPIRATION_INDEX,
              Calypso.EF_ENVIRONMENT_EXPIRATION_INDEX +
              Calypso.EF_ENVIRONMENT_EXPIRATION_LENGTH));
          */
          let expirationRaw = bitwiseAnd(
            PosSam.CALYPSO.EF_ENVIRONMENT_EXPIRATION_MASK,
            record.slice(
              PosSam.CALYPSO.EF_ENVIRONMENT_EXPIRATION_INDEX,
              PosSam.CALYPSO.EF_ENVIRONMENT_EXPIRATION_INDEX +
                PosSam.CALYPSO.EF_ENVIRONMENT_EXPIRATION_LENGTH
            )
          );

          /*
          BigInteger expirationAsBigInteger = new BigInteger(expirationRaw);
          expirationAsBigInteger = expirationAsBigInteger.shiftRight(4);
          expirationRaw = expirationAsBigInteger.toByteArray();
          */
          expirationRaw = await ByteUtils.shiftRight(expirationRaw, 4);

          // byte expirationMonthsBytes = expirationRaw.length == 1 ? expirationRaw[0] : expirationRaw[1];
          const expirationMonthsBytes =
            expirationRaw.length === 1 ? expirationRaw[0] : expirationRaw[1];

          // obtaining issue date
          // lastIssueDateBytes = Arrays.copyOfRange(record, Calypso.EF_ENVIRONMENT_ISSUE_DATE_INDEX, Calypso.EF_ENVIRONMENT_ISSUE_DATE_INDEX + Calypso.EF_ENVIRONMENT_ISSUE_DATE_LENGTH);
          lastIssueDateBytes = record.slice(
            PosSam.CALYPSO.EF_ENVIRONMENT_ISSUE_DATE_INDEX,
            PosSam.CALYPSO.EF_ENVIRONMENT_ISSUE_DATE_INDEX +
              PosSam.CALYPSO.EF_ENVIRONMENT_ISSUE_DATE_LENGTH
          );

          // expiration equals to 0x00 indicates no expiration
          // if (expirationMonthsBytes != (byte) 0x00) {
          if (expirationMonthsBytes !== 0x00) {
            // obtaining actual issue date, since lastIssueDateBytes is its
            // one's complement
            // byte[] actualIssueDate = ByteUtils.onesComplement(lastIssueDateBytes);
            // final byte[] actualIssueDate = lastIssueDateBytes;
            const actualIssueDate = lastIssueDateBytes;

            /*
            int issueDate = (actualIssueDate[0] & 0xFF) << 16
                      | (actualIssueDate[1] & 0xFF) << 8
                      | (actualIssueDate[2] & 0xFF);
            */
            const issueDate =
              ((actualIssueDate[0] & 0xff) << 16) |
              ((actualIssueDate[1] & 0xff) << 8) |
              (actualIssueDate[2] & 0xff);

            // converting issue date to millis
            // long issueDateMillisFromZeroTime = issueDate * 60 * 1_000L;
            const issueDateMillisFromZeroTime = issueDate * 60 * 1000;

            // long now = System.currentTimeMillis();
            const now = new Date();

            // adding zero time in order to obtain issue date representation
            // starting from first january 1970 at 00:00
            // long actualIssueDateMillis = issueDateMillisFromZeroTime + Calypso.ZERO_TIME_MILLIS;
            const actualIssueDateMillis =
              issueDateMillisFromZeroTime + PosSam.CALYPSO.ZERO_TIME_MILLIS;

            /*
            if(now < actualIssueDateMillis) {
                resetData();

                handlerMain.post(() -> UI.showOKDialog(getContext(), "Attenzione!",
                        "La data di inzializzazione è nel futuro!"));

                return;
            }
            */
            if (now.getTime() < actualIssueDateMillis) {
              // TODO: it's in the future! What do we do now?
              throw 'The card somehow is in the future!';
            }

            // converting issue date to calendar to add months to it
            /*
            Calendar expirationCalendar = Calendar.getInstance();
            expirationCalendar.setTimeInMillis(actualIssueDateMillis);
            expirationCalendar.add(Calendar.MONTH, (int) expirationMonthsBytes);
            */
            const expiration = new Date(actualIssueDateMillis);
            expiration.setMonth(expiration.getMonth() + expirationMonthsBytes);
            const yearsToAdd = Math.floor(expirationMonthsBytes / 12);
            expiration.setFullYear(expiration.getFullYear() + yearsToAdd);

            // if card is expired, do something
            /*
            if (now > expirationCalendar.getTimeInMillis()) {
              resetData();

              handlerMain.post(() -> UI.showOKDialog(getContext(),
                "Attenzione!",
                "La carta è scaduta!"));

              return;
            }
            */
            if (now.getTime() > expiration.getTime()) {
              // TODO: card expired! What do we do now?
              throw 'Card expired!';
            }
          }

          // card is not expired, checking data format and card circuit values

          // byte dataFormat = record[Calypso.EF_ENVIRONMENT_DATA_FORMAT_INDEX];
          const dataFormat =
            record[PosSam.CALYPSO.EF_ENVIRONMENT_DATA_FORMAT_INDEX];

          // byte cardCircuit = record[Calypso.EF_ENVIRONMENT_CARD_CIRCUIT_INDEX];
          const cardCircuit =
            record[PosSam.CALYPSO.EF_ENVIRONMENT_CARD_CIRCUIT_INDEX];

          /*
          if (dataFormat != Calypso.CARD_DATA_FORMAT ||
                  cardCircuit != Calypso.CARD_BIP_CIRCUIT) {
              throw new IllegalStateException("Wrong card's data format or " +
                      "card's circuit");
          }
          */
          if (
            dataFormat !== PosSam.CALYPSO.CARD_DATA_FORMAT ||
            cardCircuit !== PosSam.CALYPSO.CARD_BIP_CIRCUIT
          ) {
            // TODO: Wrong card data format! What do we do here?
            throw 'Wrong card data format or card circuit!';
          }

          // byte[] taxCodeBytes = Arrays.copyOfRange(record, Calypso.EF_ENVIRONMENT_TAX_CODE_INDEX, Calypso.EF_ENVIRONMENT_TAX_CODE_INDEX + Calypso.EF_ENVIRONMENT_TAX_CODE_LENGTH);
          const taxCodeBytes = record.slice(
            PosSam.CALYPSO.EF_ENVIRONMENT_TAX_CODE_INDEX,
            PosSam.CALYPSO.EF_ENVIRONMENT_TAX_CODE_INDEX +
              PosSam.CALYPSO.EF_ENVIRONMENT_TAX_CODE_LENGTH
          );

          // String taxCode = new String(taxCodeBytes);
          const taxCode = await ByteUtils.stringFromByteArray(taxCodeBytes);

          console.warn(taxCode);

          // if the tax code saved on the card and the one saved on the server
          // are different,
          // show user data as not initialized to make sure user save data again,
          // saving the correct tax code on the card if the server one is not empty

          /*
          if (!taxCode.equalsIgnoreCase(lastUserData.getTaxCode())) {
              if (lastUserData.getTaxCode() == null ||
                      lastUserData.getTaxCode().isEmpty())
                  lastUserData.setTaxCode(taxCode);

              lastUserData.setInitialized(false);
          }
          */
          if (taxCode.toLowerCase() !== data.fiscal_code) {
            // TODO: set the card as not initialized
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
