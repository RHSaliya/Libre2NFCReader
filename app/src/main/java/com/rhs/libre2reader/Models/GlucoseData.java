
package com.rhs.libre2reader.Models;

import androidx.annotation.NonNull;

import com.rhs.libre2reader.Utils.DecryptionUtils;

import java.text.DecimalFormat;

public class GlucoseData implements Comparable<GlucoseData> {
    public static long startFrom;
    public static int delay;

    public long realDate;                 // The time of this reading in ms
    public int glucoseLevel = -1;         // The bg value that was calculated by the oop algorithm.
    public int glucoseLevelRaw = -1;
    public int temp;


    public static GlucoseData fromFram(int[] fram, int offset, long startDate, int age, int i, boolean isTrend) {
        GlucoseData data = new GlucoseData();
        data.setGlucoseLevelRaw(readBits(fram, offset, 0, 0xe));
        data.setTemp(readBits(fram, offset, 0x1a, 0xc) << 2);
        long date = isTrend ? (startDate + (age - i) * 60 * 1000L) : ((age - delay - i * 15) > -1 ? startFrom - (long) i * 15 * 60 * 1000 : startDate);
        data.setRealDate(date);
        data.setGlucoseLevel(data.getGlucoseLevelRaw() / 10);
        return data;
    }

    public GlucoseData() {
    }

    @NonNull
    public String toString() {
        return "{ glucoseLevel = " + glucoseLevel + " glucoseLevelRaw = " + glucoseLevelRaw +
                " glucoseLevel " + glucoseLevel + " realDate " + realDate + "}";
    }

    public String glucose(boolean mmol) {
        return glucose(glucoseLevel, mmol);
    }

    public static String glucose(int mgdl, boolean mmol) {
        return mmol ? new DecimalFormat("##.0").format(mgdl / DecryptionUtils.MMOLL_TO_MGDL) : String.valueOf(mgdl);
    }

    @Override
    public int compareTo(GlucoseData another) {
        return (int) (realDate - another.realDate);
    }

    public long getRealDate() {
        return realDate;
    }

    public void setRealDate(long realDate) {
        this.realDate = realDate;
    }


    public int getGlucoseLevel() {
        return glucoseLevel;
    }

    public void setGlucoseLevel(int glucoseLevel) {
        this.glucoseLevel = glucoseLevel;
    }

    public int getGlucoseLevelRaw() {
        return glucoseLevelRaw;
    }

    public void setGlucoseLevelRaw(int glucoseLevelRaw) {
        this.glucoseLevelRaw = glucoseLevelRaw;
    }


    public int getTemp() {
        return temp;
    }

    public void setTemp(int temp) {
        this.temp = temp;
    }

    private static int readBits(int[] buffer, int byteOffset, int bitOffset, int bitCount) {
        if (bitCount == 0) {
            return 0;
        }
        int res = 0;
        for (int i = 0; i < bitCount; i++) {
            final int totalBitOffset = byteOffset * 8 + bitOffset + i;
            final int byte1 = (int) Math.floor(totalBitOffset / 8);
            final int bit = totalBitOffset % 8;
            if (totalBitOffset >= 0 && ((buffer[byte1] >> bit) & 0x1) == 1) {
                res = res | (1 << i);
            }
        }
        return res;
    }

}
