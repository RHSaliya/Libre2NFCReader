package com.rhs.libre2reader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rhs.libre2reader.Utils.DecryptionUtils;
import com.rhs.libre2reader.Utils.LibreUtils;
import com.rhs.libre2reader.Utils.NfcVReaderTask;

public class MainActivity extends AppCompatActivity {
    TextView tvData;
    NfcAdapter nfcAdapter;
    boolean useDummyData = false;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvData = findViewById(R.id.tvData);

        if (useDummyData){
            byte[] patchInfo = new byte[]{(byte)0x9d, 0x08, 0x30, 0x08, (byte)0x4c, 0x20};
            byte[] sensorUid = new byte[]{0x5d, 0x26, (byte)0xd1, 0x04, 0x00, (byte)0xa4, 0x07, (byte)0xe0};
            int[] data = new int[]{
                    84, 227, 138, 211, 215, 173, 236, 172, 203, 186, 5, 17, 165, 203, 193, 252, 91, 77, 230, 51, 94, 26, 231, 81, 71, 164, 85, 230, 178, 126, 202, 81, 232, 104, 185, 198, 232, 139, 163, 133, 214, 118, 156, 95, 134, 168, 205, 215, 163, 8, 82, 79, 187, 32, 218, 214, 194, 33, 175, 154, 106, 116, 32, 224, 186, 47, 77, 91, 245, 152, 69, 126, 108, 35, 45, 148, 2, 253, 255, 8, 52, 216, 252, 204, 17, 74, 195, 33, 14, 198, 218, 21, 191, 8, 166, 115, 135, 213, 127, 57, 50, 185, 135, 245, 38, 225, 141, 236, 222, 235, 117, 165, 136, 171, 14, 218, 139, 86, 190, 194, 109, 161, 110, 101, 188, 50, 4, 168, 11, 192, 0, 148, 200, 122, 112, 10, 163, 223, 40, 145, 235, 43, 143, 89, 196, 113, 216, 56, 13, 48, 167, 3, 34, 72, 8, 237, 94, 13, 18, 166, 134, 115, 71, 202, 1, 231, 16, 32, 214, 176, 111, 24, 184, 131, 17, 39, 164, 37, 246, 70, 106, 59, 10, 23, 79, 59, 86, 174, 131, 108, 153, 69, 168, 234, 86, 39, 149, 173, 230, 246, 143, 187, 141, 243, 245, 96, 83, 243, 71, 80, 230, 31, 38, 88, 97, 252, 39, 179, 193, 125, 38, 60, 5, 11, 173, 119, 9, 95, 201, 232, 43, 42, 101, 105, 156, 179, 32, 235, 169, 120, 161, 210, 21, 214, 217, 98, 29, 238, 72, 63, 3, 4, 75, 235, 104, 79, 165, 72, 20, 23, 114, 123, 208, 215, 24, 243, 117, 36, 222, 30, 182, 12, 87, 160, 133, 4, 44, 50, 79, 41, 76, 190, 255, 159, 75, 171, 20, 123, 228, 17, 147, 240, 15, 27, 11, 57, 68, 153, 191, 36, 227, 83, 249, 173, 247, 211, 35, 118, 129, 244, 53, 202, 14, 165, 67, 173, 93, 219, 137, 152, 9, 211, 211, 126, 54, 67, 93, 84, 163, 223, 202, 33, 187, 41, 69, 242, 54, 177, 209, 251, 107, 208, 114, 52
            };
            String serialNo = LibreUtils.decodeSerialNumberKey(sensorUid);
            int[] fram = DecryptionUtils.decryptFRAM(sensorUid, patchInfo, data);
            DecryptionUtils.extractFRAMData(fram, (trend, history, age, startDate, maxAge) -> {
                tvData.setText(String.format("Glucose Raw: %d \n", trend.get(0).glucoseLevelRaw));
                tvData.append(String.format("\nSerial No : %s", serialNo));
                tvData.append(String.format("\nPatch Info : %s", DecryptionUtils.bytesToHex(patchInfo)));
                tvData.append(String.format("\nSensor UID : %s", DecryptionUtils.bytesToHex(sensorUid)));
                tvData.append(String.format("\nAge : %s", DecryptionUtils.timeFormat(age)));
                tvData.append(String.format("\nMax Age : %s", DecryptionUtils.timeFormat(maxAge)));
                tvData.append(String.format("\nStart Date : %s", DecryptionUtils.sdf.format(startDate)));
                tvData.append(String.format("\nExpiry Date : %s", DecryptionUtils.sdf.format(startDate + maxAge * 60 * 1000L)));
            });
        } else {
            tvData.setText("Tap device on Libre2 sensor");
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            readNFCData();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.e("@@@@@@", "Detected NFC data");
        new NfcVReaderTask((trend, history, serialNo, pathInfo, sensorUid, age, startDate, maxAge, succeeded, error) -> {
            if (succeeded) {
                tvData.setText(String.format("Glucose Raw: %d \n", trend.get(0).glucoseLevelRaw));
                tvData.append(String.format("\nSerial No : %s", serialNo));
                tvData.append(String.format("\nPatch Info : %s", pathInfo));
                tvData.append(String.format("\nSensor UID : %s", sensorUid));
                tvData.append(String.format("\nAge : %s", DecryptionUtils.timeFormat(age)));
                tvData.append(String.format("\nMax Age : %s", DecryptionUtils.timeFormat(maxAge)));
                tvData.append(String.format("\nStart Date : %s", DecryptionUtils.sdf.format(startDate)));
                tvData.append(String.format("\nExpiry Date : %s", DecryptionUtils.sdf.format(startDate + maxAge * 60 * 1000L)));
            } else {
                tvData.setText(error);
            }
        }).execute(tag);
    }

    private void readNFCData() {
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is not enabled or supported", Toast.LENGTH_SHORT).show();
            return;
        }
        int flags = NfcAdapter.FLAG_READER_NFC_V
                | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;
        final Bundle options = new Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 5000);
        nfcAdapter.enableReaderMode(this, tag -> new NfcVReaderTask((trend, history, serialNo, pathInfo, sensorUid, age, startDate, maxAge, succeeded, error) -> {
            if (succeeded) {
                tvData.setText(String.format("Glucose Raw: %d \n", trend.get(0).glucoseLevelRaw));
                tvData.append(String.format("\nSerial No : %s", serialNo));
                tvData.append(String.format("\nPatch Info : %s", pathInfo));
                tvData.append(String.format("\nSensor UID : %s", sensorUid));
                tvData.append(String.format("\nAge : %s", DecryptionUtils.timeFormat(age)));
                tvData.append(String.format("\nMax Age : %s", DecryptionUtils.timeFormat(maxAge)));
                tvData.append(String.format("\nStart Date : %s", DecryptionUtils.sdf.format(startDate)));
                tvData.append(String.format("\nExpiry Date : %s", DecryptionUtils.sdf.format(startDate + maxAge * 60 * 1000L)));
            } else {
                tvData.setText(error);
            }
        }).execute(tag), flags, options);
    }
}