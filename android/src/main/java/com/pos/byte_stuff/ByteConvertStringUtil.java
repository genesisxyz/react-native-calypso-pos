package com.pos.byte_stuff;

/**
 *  @author john
 *  Convert byte[] to hex string
 * */
public class ByteConvertStringUtil {
	public static String bytesToHexString(byte[] src){
		StringBuilder stringBuilder = new StringBuilder("");
	    if (src == null || src.length <= 0) {
	        return null;
	    }
	    for (int i = 0; i < src.length; i++) {
	        int v = src[i] & 0xFF;
	        String hv = Integer.toHexString(v);
	        if (hv.length() < 2) {
	            stringBuilder.append(0);
	        }
	        stringBuilder.append(hv).append(" ");
	    }
	    return stringBuilder.toString();
	}

	public static String bytesToHexString(byte[] src, int from){
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = from; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv).append(" ");
		}
		return stringBuilder.toString();
	}

	public static String byteToHexString(byte src){
		int v = src & 0xFF;
		String result = Integer.toHexString(v);
		if(result.length() == 2)
			return result;

		return "0" + result;
	}

	public static void stringToByteArray(String strInput, byte[] arryByte) {
		strInput = strInput.trim();
		String[] arryString = strInput.split("\\s+");
		if (arryByte.length < arryString.length) {
			throw new RuntimeException("Something is wrong with this string!");
		} else {
			for(int i = 0; i < arryString.length; ++i) {
				if (!CheckString(arryString[i])) {
					throw new RuntimeException("Something is wrong with this string!");
				}

				arryByte[i] = StringToByte(arryString[i]);
			}
		}
	}

	public static byte[] stringToByteArray(String strInput) {
		String arryString = strInput.replaceAll("\\s+", "");
		if(arryString.length() % 2 != 0)
			throw new RuntimeException("Wrong string length!");

		byte[] bytes = new byte[arryString.length() / 2];

		for(int i = 0; i < arryString.length();) {
			String substring = arryString.substring(i, i + 2);
			if (!CheckString(substring)) {
				throw new RuntimeException("Something is wrong with this string!");
			}

			bytes[i / 2] = StringToByte(substring);

			i += 2;
		}

		return bytes;
	}

	private static boolean CheckString(String strInput) {
		strInput = strInput.trim();
		if (strInput.length() != 2) {
			return false;
		} else {
			byte[] byteArry = strInput.getBytes();

			for(int i = 0; i < 2; ++i) {
				if (!CheckByte(byteArry[i])) {
					return false;
				}
			}

			return true;
		}
	}

	protected static boolean CheckByte(byte byteIn) {
		if (byteIn <= 57 && byteIn >= 48) {
			return true;
		} else if (byteIn <= 70 && byteIn >= 65) {
			return true;
		} else {
			return byteIn <= 102 && byteIn >= 97;
		}
	}

	protected static byte StringToByte(String strInput) {
		byte[] byteArry = strInput.getBytes();

		for(int i = 0; i < 2; ++i) {
			if (byteArry[i] <= 57 && byteArry[i] >= 48) {
				byteArry[i] = (byte)(byteArry[i] - 48);
			} else if (byteArry[i] <= 70 && byteArry[i] >= 65) {
				byteArry[i] = (byte)(byteArry[i] - 55);
			} else if (byteArry[i] <= 102 && byteArry[i] >= 97) {
				byteArry[i] = (byte)(byteArry[i] - 87);
			}
		}

		return (byte)(byteArry[0] << 4 | byteArry[1] & 15);
	}
}
