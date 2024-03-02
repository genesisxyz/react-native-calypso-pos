// FILEPATH: /Users/genesisxyz/dev/react-native/react-native-pos/src/Pos.test.ts
import { ErrorCode } from '../Pos';

jest.mock('react-native', () => {
  const rn = jest.requireActual('react-native');
  rn.NativeModules.Pos = {
    // Mock implementation of your Pos methods
  };
  return rn;
});

describe('ErrorCode Enum', () => {
  it('should have the correct values', () => {
    expect(ErrorCode.Unknown).toEqual('UNKNOWN');
    expect(ErrorCode.CardNotSupported).toEqual('CARD_NOT_SUPPORTED');
    expect(ErrorCode.CardNotPresent).toEqual('CARD_NOT_PRESENT');
    expect(ErrorCode.CardNotConnected).toEqual('CARD_NOT_CONNECTED');
    expect(ErrorCode.CardConnectFail).toEqual('CARD_CONNECT_FAIL');
    expect(ErrorCode.CardDisconnectFail).toEqual('CARD_DISCONNECT_FAIL');
    expect(ErrorCode.TransmitApduCommand).toEqual('TRANSMIT_APDU_COMMAND');
    expect(ErrorCode.PendingRequest).toEqual('PENDING_REQUEST');
    expect(ErrorCode.SamConnectFail).toEqual('SAM_CONNECT_FAIL');
    expect(ErrorCode.SamDisconnectFail).toEqual('SAM_DISCONNECT_FAIL');
    expect(ErrorCode.Cancel).toEqual('CANCEL');
  });
});
