package com.rhs.libre2reader.Utils;


import android.util.Log;

import androidx.annotation.NonNull;

import com.rhs.libre2reader.Models.GlucoseData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DecryptionUtils {
    public static final double MMOLL_TO_MGDL = 18.0182;
    public static final double MGDL_TO_MMOLL = 1 / MMOLL_TO_MGDL;
    public static final int LIBRE_1_2_FRAM_SIZE = 344;

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static final int[] key = new int[]{0xA0C5, 0x6860, 0x0000, 0x14C6};

    public static int UInt16(int i1) {
        return i1 & 0xffff;
    }

    public static int UInt8(int i1) {
        return i1 & 0xff;
    }

    public static int UInt16(int i1, int i2) {
        return (UInt8(i1) << 8) + UInt8(i2);
    }

    public static int getArg(byte[] patchInfo) {
        return UInt16(patchInfo[5], patchInfo[4]) ^ 0x44;
    }

    public static int[] prepareVariables(byte[] id, int x, int y) {
        int[] var = new int[4];
        var[0] = UInt16(UInt16(id[5], id[4]) + x + y);
        var[1] = UInt16(id[3], id[2]) + key[2];
        var[2] = UInt16(id[1], id[0]) + x * 2;
        var[3] = (0x241a ^ key[3]);
        return var;
    }

    private static int op(int value) {
        // We check for last 2 bits and do the xor with specific value if bit is 1
        int res = value >> 2;// Result does not include these last 2 bits

        if ((value & 1) != 0) { // If last bit is 1
            res = res ^ key[1];
        }

        if ((value & 2) != 0) { // If second last bit is 1
            res = res ^ key[0];
        }

        return res;
    }

    public static int[] processCrypto(int[] input) {
        int r0 = op(input[0]) ^ input[3];
        int r1 = op(r0) ^ input[2];
        int r2 = op(r1) ^ input[1];
        int r3 = op(r2) ^ input[0];
        int r4 = op(r3);
        int r5 = op(r4 ^ r0);
        int r6 = op(r5 ^ r1);
        int r7 = op(r6 ^ r2);


        int f1 = r0 ^ r4;
        int f2 = r1 ^ r5;
        int f3 = r2 ^ r6;
        int f4 = r3 ^ r7;

        return new int[]{f4, f3, f2, f1};
    }


    public static int[] decryptFRAM(byte[] uid, byte[] patchInfo, int[] data) {
        Log.e("@@@@@@@@@", Arrays.toString(data));
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 43; i++) {
            int[] input = prepareVariables(uid, i, getArg(patchInfo));
            int[] blockKey = processCrypto(input);

            result.add(data[i * 8] ^ UInt8(blockKey[0]));
            result.add(data[i * 8 + 1] ^ UInt8(blockKey[0] >> 8));
            result.add(data[i * 8 + 2] ^ UInt8(blockKey[1]));
            result.add(data[i * 8 + 3] ^ UInt8(blockKey[1] >> 8));
            result.add(data[i * 8 + 4] ^ UInt8(blockKey[2]));
            result.add(data[i * 8 + 5] ^ UInt8(blockKey[2] >> 8));
            result.add(data[i * 8 + 6] ^ UInt8(blockKey[3]));
            result.add(data[i * 8 + 7] ^ UInt8(blockKey[3] >> 8));
        }


        int[] array = new int[result.size()];
        for (int i = 0; i < result.size(); i++) array[i] = result.get(i);
        return array;
    }

    public static final SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa, d MMM yyyy", Locale.ENGLISH);

    public static String timeFormat(int minutes) {
        int hours = minutes / 60;
        int days = hours / 24;
        return days + " days " + hours % 24 + " hours " + minutes % 60 + " minutes";
    }

    public static void extractFRAMData(@NonNull int[] fram, @NonNull OnDataReceivedListener onDataReceivedListener) {
        GlucoseData.startFrom = System.currentTimeMillis();
        int age = (fram[317] << 8) + fram[316]; // In Minutes
        long startDate = GlucoseData.startFrom - age * 60L * 1000L; // Milliseconds
        int maxAge = (fram[327] << 8) + fram[326]; // In Minutes
        int trendIndex = fram[26];  // Trend index
        int historyIndex = fram[27]; // History index

        Log.e("@@@@@@@@", "age: " + timeFormat(age) + " startDate: " + sdf.format(startDate) + " maxAge: " + timeFormat(maxAge) + " trendIndex: " + trendIndex + " historyIndex: " + historyIndex);

        // Trend
        final List<GlucoseData> trend = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            int j = trendIndex - 1 - i;
            if (j < 0) {
                j += 16;
            }
            int offset = 28 + j * 6;
            GlucoseData glucoseData = GlucoseData.fromFram(fram, offset, startDate, age, i, true);
            trend.add(glucoseData);
        }

        //History
        GlucoseData.delay = ((age - 3) % 15 + 3);
        final List<GlucoseData> history = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            int j = historyIndex - 1 - i;
            if (j < 0) {
                j += 32;
            }
            int offset = 124 + j * 6;
            GlucoseData glucoseData = GlucoseData.fromFram(fram, offset, startDate, age, i, false);
            history.add(glucoseData);
        }

        onDataReceivedListener.onDataReceived(trend, history, age, startDate, maxAge);
    }

    public interface OnDataReceivedListener {
        void onDataReceived(List<GlucoseData> trend, List<GlucoseData> history, int age, long startDate, int maxAge);
    }
}
