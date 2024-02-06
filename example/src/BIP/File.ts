// MARK: check PDF 'Requisiti-smart-card-per-il-progetto-BIP-v3.1.4 (5)' from section 2.5.2
export abstract class File {
  protected abstract record: Uint8Array;

  protected abstract readonly RECORD_BYTE_LENGTH: number;

  public static SFI: number;

  public static AID = new Uint8Array([
    0x31, 0x54, 0x49, 0x43, 0x2e, 0x49, 0x43, 0x41, 0xd3, 0x80, 0x12, 0x00, 0x91, 0x01,
  ]);

  protected get recordHex() {
    return Array.from(this.record).map((e) => e.toString(16).padStart(2, '0'));
  }

  public get readableRecord() {
    return this.recordHex.join(' ');
  }
}

export function mod(n: number, m: number) {
  return ((n % m) + m) % m;
}

export function getBytes(recordHex: string[], offset: number, limit: number) {
  return recordHex.join('').slice(offset * 2, limit * 2);
}

export function byteArrayFromHex(recordHex: string) {
  return new Uint8Array(recordHex.match(/../g)!.map((e) => parseInt(e, 16)));
}
