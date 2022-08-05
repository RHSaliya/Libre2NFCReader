package com.rhs.libre2reader.Utils;

import static com.rhs.libre2reader.Utils.DecryptionUtils.extractFRAMData;

import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.rhs.libre2reader.Models.GlucoseData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NfcVReaderTask extends AsyncTask<Tag, Void, Tag> {
    private static final String TAG = "NfcVReaderTask";
    OnNfcVReadComplete listener;
    String serialNoString = "", patchInfoString = "", sensorUidString = "";
    List<GlucoseData> trendList = new ArrayList<>();
    List<GlucoseData> historyList = new ArrayList<>();


    public NfcVReaderTask(@NonNull OnNfcVReadComplete listener) {
        this.listener = listener;
    }

    private byte[] data = new byte[360];

    @Override
    protected Tag doInBackground(Tag... params) {
        try {
            Tag tag = params[0];
            byte[] sensorUid = tag.getId();
            serialNoString = LibreUtils.decodeSerialNumberKey(sensorUid);
            sensorUidString = DecryptionUtils.bytesToHex(sensorUid);
            Log.e("@@@@@@@@", "serialNo = " + serialNoString);
            Log.e("@@@@@@@@", "sensorUid = " + sensorUidString);

            NfcV nfcvTag = NfcV.get(tag);
            try {
                try {
                    nfcvTag.connect();
                } catch (IOException e) {
                    Log.e(TAG, "Trying second nfc connect");
                    Thread.sleep(250);
                    nfcvTag.connect();
                }
                long time_patch = System.currentTimeMillis();
                byte[] patchInfo;
                while (true) {
                    try {
                        final byte[] cmd = new byte[]{0x02, (byte) 0xa1, 0x07};
                        patchInfo = nfcvTag.transceive(cmd);
                        if (patchInfo != null) {
                            // We need to throw away the first byte.
                            patchInfo = Arrays.copyOfRange(patchInfo, 1, patchInfo.length);
                        }
                        break;
                    } catch (IOException e) {
                        if ((System.currentTimeMillis() > time_patch + 2000)) {
                            Log.e(TAG, "patchInfo tag read timeout");
                            listener.onDataReceived(trendList, historyList, serialNoString, patchInfoString, sensorUidString, 0, 0, 0, false, "patchInfo tag read timeout");
                            return null;
                        }
                        Thread.sleep(100);
                    }
                }
                patchInfoString = DecryptionUtils.bytesToHex(patchInfo);
                Log.e("@@@@@@@@", "patchInfo = " + patchInfoString);

                {
                    int correct_reply_size;
                    for (int i = 0; i < 43; i++) {
                        final byte[] cmd;
                        int startBlock;

                        cmd = new byte[]{(byte) 0x02, (byte) 0x23, 0, (byte) 0x0};
                        correct_reply_size = 9;
                        startBlock = 1;

                        cmd[2] = (byte) i;

                        byte[] oneBlock;
                        long time = System.currentTimeMillis();
                        while (true) {
                            try {
                                Log.e(TAG, "sending command " + DecryptionUtils.bytesToHex(cmd));
                                oneBlock = nfcvTag.transceive(cmd);
                                break;
                            } catch (IOException e) {
                                if ((System.currentTimeMillis() > time + 2000)) {
                                    Log.e(TAG, "tag read timeout");
                                    listener.onDataReceived(trendList, historyList, serialNoString, patchInfoString, sensorUidString, 0, 0, 0, false, "tag read timeout");
                                    return null;
                                }
                                Thread.sleep(100);
                            }
                        }
                        if (oneBlock.length != correct_reply_size) {
                            Log.e(TAG, "Incorrect block size: " + oneBlock.length + " vs " + correct_reply_size);
                            listener.onDataReceived(trendList, historyList, serialNoString, patchInfoString, sensorUidString, 0, 0, 0, false, "Incorrect block size: " + oneBlock.length + " vs " + correct_reply_size);
                            return null;
                        }
                        System.arraycopy(oneBlock, startBlock, data, i * 8, 8);
                    }
                }

                data = Arrays.copyOf(data, DecryptionUtils.LIBRE_1_2_FRAM_SIZE);
                Log.e(TAG, "GOT TAG DATA: " + DecryptionUtils.bytesToHex(data));

                int[] intData = new int[data.length];
                for (int i = 0; i < data.length; i++) {
                    intData[i] = data[i] & 0xff;
                }
                int[] decrData = DecryptionUtils.decryptFRAM(sensorUid, patchInfo, intData);
                extractFRAMData(decrData, (trend, history, age, startDate, maxAge) -> listener.onDataReceived(trend, history, serialNoString, patchInfoString, sensorUidString, age, startDate, maxAge, true, "None"));
            } catch (Exception e) {
                Log.e("ScannedDataFromNfc", "Got exception reading nfc in background: ", e);
                listener.onDataReceived(trendList, historyList, serialNoString, patchInfoString, sensorUidString, 0, 0, 0, false, "Got exception reading nfc in background: " + e.getLocalizedMessage());
                return null;
            } finally {
                try {
                    nfcvTag.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing tag!");
                    listener.onDataReceived(trendList, historyList, serialNoString, patchInfoString, sensorUidString, 0, 0, 0, false, "Error closing tag: " + e.getLocalizedMessage());
                }
            }
            return tag;
        } catch (Exception e) {
            Log.e(TAG, "Error: "+e.getLocalizedMessage());
            return null;
        }
    }

    public interface OnNfcVReadComplete {
        void onDataReceived(List<GlucoseData> trend, List<GlucoseData> history, String serialNo, String pathInfo, String sensorUid, int age, long startDate, int maxAge, boolean succeeded, String error);
    }

}