package com.pos.byteUtils;

import android.os.Build;

import java.nio.ByteBuffer;

public class ByteUtils {

    public static byte[] bitwiseAnd(byte[] array1, byte[] array2) {
        if(array1.length != array2.length)
            return null;

        byte[] result = array1.clone();
        for(int i = 0; i < array1.length; i++) {
            result[i] &= array2[i];
        }

        return result;
    }

    public static byte[] bitwiseOr(byte[] array1, byte[] array2) {
        if(array1.length != array2.length)
            return null;

        byte[] result = array1.clone();
        for(int i = 0; i < array1.length; i++) {
            result[i] |= array2[i];
        }

        return result;
    }

    public static byte[] onesComplement(byte[] input) {
        byte[] result = input.clone();
        for(int i = 0; i < result.length; i++)
            result[i] = (byte)(((int)result[i] * -1) - 1);

        return result;
    }

    public static byte onesComplement(byte input) {
        return (byte)(((int)input * -1) - 1);
    }

    public static byte[] longToBytes(long input) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return longToBytes(input, Long.BYTES);
        else
            return longToBytes(input, 8);
    }

    public static byte[] longToBytes(long input, int capacity) {
        ByteBuffer buffer = ByteBuffer.allocate(capacity);
        buffer.putLong(input);
        return buffer.array();
    }

    public static byte[] intToBytes(int input, int capacity) {
        ByteBuffer buffer = ByteBuffer.allocate(capacity);
        buffer.putInt(input);
        return buffer.array();
    }
}
