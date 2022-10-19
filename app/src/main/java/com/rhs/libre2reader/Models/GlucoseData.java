
package com.rhs.libre2reader.Models;

import android.util.Log;

import androidx.annotation.NonNull;

import com.rhs.libre2reader.Utils.DecryptionUtils;

import java.text.DecimalFormat;
import java.util.Date;

public class GlucoseData implements Comparable<GlucoseData> {
    public static long startFrom;
    public static int delay;

    public int id;
    public Date date;
    public int rawValue;
    public int rawTemperature;
    public int temperatureAdjustment;
    public boolean hasError;
    public int value = 0;
    public double temperature = 0;


    public static GlucoseData fromFram(int[] fram, int offset, long startDate, int age, int i, boolean isTrend) {
        GlucoseData data = new GlucoseData();
        data.rawValue = readBits(fram, offset, 0, 0xe);
        data.value = data.rawValue / 10;
        // data.quality = DecryptionUtils.UInt16(readBits(fram, offset, 0xe, 0xb)) & 0x1FF;
        // data.qualityFlags = (readBits(fram, offset, 0xe, 0xb) & 0x600) >> 9;
        data.hasError = readBits(fram, offset, 0x19, 0x1) != 0;
        data.rawTemperature = readBits(fram, offset, 0x1a, 0xc) << 2;
        data.temperatureAdjustment = readBits(fram, offset, 0x26, 0x9) << 2;
        final int negativeAdjustment = readBits(fram, offset, 0x2f, 0x1);
        if (negativeAdjustment != 0) {
            data.temperatureAdjustment = -data.temperatureAdjustment;
        }
        data.id = age - i;

        long date = isTrend ? (startDate + (age - i) * 60 * 1000L) : ((age - delay - i * 15) > -1 ? startFrom - (long) i * 15 * 60 * 1000 : startDate);
        data.date = new Date(date);
        Log.e("@@@@@@@@@@@", ""
                + data.rawValue);
        return data;
    }

    public GlucoseData() {
    }

    @NonNull
    public String toString() {
        return "{ glucoseLevel = " + value + " glucoseLevelRaw = " + rawValue + " realDate " + date.toString() + "}";
    }

    public String glucose(boolean mmol) {
        return glucose(value, mmol);
    }

    public static String glucose(int mgdl, boolean mmol) {
        return mmol ? new DecimalFormat("##.0").format(mgdl / DecryptionUtils.MMOLL_TO_MGDL) : String.valueOf(mgdl);
    }

    @Override
    public int compareTo(GlucoseData another) {
        return date.compareTo(another.date);
    }

    public int getGlucoseLevelRaw() {
        return rawValue;
    }

    private static int readBits(int[] buffer, int byteOffset, int bitOffset, int bitCount) {
        if (bitCount == 0) {
            return 0;
        }
        int res = 0;
        for (int i = 0; i < bitCount; i++) {
            final int totalBitOffset = byteOffset * 8 + bitOffset + i;
            final int byte1 = (int) Math.floor(totalBitOffset / 8f);
            final int bit = totalBitOffset % 8;
            if (totalBitOffset >= 0 && ((buffer[byte1] >> bit) & 0x1) == 1) {
                res = res | (1 << i);
            }
        }
        return res;
    }

    public static CalibrationInfo getCalibrationInfo(int[] fram) {
        CalibrationInfo calibrationInfo = new CalibrationInfo();
        boolean negativei3 = GlucoseData.readBits(fram, 0x150, 0x21, 1) != 0;

        calibrationInfo.i1 = GlucoseData.readBits(fram, 2, 0, 3);
        calibrationInfo.i2 = GlucoseData.readBits(fram, 2, 3, 0xa);
        calibrationInfo.i3 = (negativei3 ? -1 : 1) * GlucoseData.readBits(fram, 0x150, 0, 8);
        calibrationInfo.i4 = GlucoseData.readBits(fram, 0x150, 8, 0xe);
        calibrationInfo.i5 = GlucoseData.readBits(fram, 0x150, 0x28, 0xc) << 2;
        calibrationInfo.i6 = GlucoseData.readBits(fram, 0x150, 0x34, 0xc) << 2;

        return calibrationInfo;
    }

}
