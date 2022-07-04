package com.example.bloodflow.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bloodflow.DatabaseHandler;
import com.example.bloodflow.FormatCorrector;
import com.example.bloodflow.MultipartUtility;
import com.example.bloodflow.R;
import com.example.bloodflow.adapter.CustomAdapter;
import com.example.bloodflow.adapter.DbListAdapter;
import com.example.bloodflow.model.Test;
import com.example.bloodflow.utils.RandomUtils;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    String tempFileName = null;
    boolean STOP = false;
    String serialStr = "";
    TextView viewHeartBPM;
    TextView viewSpo2;
    ImageView imgBeat;
    int cVisit = 0;
    SharedPreferences sharedPreferences;
    BluetoothSocket btSocket;
    Timer timer;
    DatabaseHandler db;
    ListView dbList;
    LinearLayout dbTitleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Disable night mode
        setContentView(R.layout.activity_main);

        db = new DatabaseHandler(this);

        dbList = findViewById(R.id.dbList);
        dbTitleList = findViewById(R.id.dbTitleList);
        dbListUpdate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);

        timer = new Timer();
        timer.schedule(new connectUpdate(), 0, 1000);

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        if (isConnected()) {
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        deleteTemp(new File(RandomUtils.TEMP));
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                btSocket = null;
            }
        }
    };
    class connectUpdate extends TimerTask {
        public void run() {
            runOnUiThread(MainActivity.this::viewConnect);
        }
    }

    public void clickConnect(View v) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!isBluetoothEnabled(btAdapter)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { System.out.println("NOT_GRANTED"); }
            btAdapter.enable();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.bt_list_dialog, null);
        ListView btPairedList = row.findViewById(R.id.btPairedList);
        btPairedList.setAdapter(new CustomAdapter(this));
        builder.setView(row);
        AlertDialog dialog = builder.create();
        dialog.show();
        btPairedList.setOnItemClickListener((adapterView, view, i, l) -> {
            if (ActivityCompat.checkSelfPermission(row.getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { System.out.println("NOT_GRANTED"); }
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            List<String> sMacs = new ArrayList<>();
            for (BluetoothDevice bt : pairedDevices) {
                sMacs.add(bt.getAddress());
            }
            String[] macs = sMacs.toArray(new String[0]);

            sharedPreferences = getSharedPreferences(RandomUtils.KEY, 0);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("MAC", macs[i]);
            editor.apply();

            btDeviceConnect();
            dialog.dismiss();
        });
    }
    public void clickAddDoctor(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View d = inflater.inflate(R.layout.add_doctor_dialog, null);
        EditText inputDoctorID = d.findViewById(R.id.inputDoctorID);
        EditText inputName = d.findViewById(R.id.inputName);
        Button btnSave = d.findViewById(R.id.btnSave);
        builder.setView(d);
        AlertDialog dialog = builder.create();
        SharedPreferences p = getSharedPreferences(RandomUtils.KEY, 0);
        String doctorID = p.getString("doctorID", RandomUtils.EMPTY);
        String userName = p.getString("userName", RandomUtils.EMPTY);
        if (!doctorID.equals(RandomUtils.EMPTY)) {
            inputDoctorID.setText(doctorID);
            inputName.setText(userName);
        }
        dialog.show();

        btnSave.setOnClickListener(view -> {
            String strInputDoctorID = inputDoctorID.getText().toString();
            String strInputName = inputName.getText().toString();
            if (TextUtils.isEmpty(strInputDoctorID) || TextUtils.isEmpty(strInputName)) {
                if (TextUtils.isEmpty(strInputDoctorID)) {
                    inputDoctorID.setError("Please type the doctor ID");
                }
                if (TextUtils.isEmpty(strInputName)) {
                    inputName.setError("Please type your name");
                }
            } else {
                SharedPreferences sP = getSharedPreferences(RandomUtils.KEY, 0);
                SharedPreferences.Editor editor = sP.edit();
                editor.putString("doctorID", strInputDoctorID);
                editor.putString("userName", strInputName);
                editor.apply();
                dialog.dismiss();
                Toast.makeText(d.getContext(), R.string.saved, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void clickSendList(View v) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.storage_permission, Toast.LENGTH_LONG).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.storage_permission, Toast.LENGTH_LONG).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View bb = inflater.inflate(R.layout.send_extract_dialog, null);
        Button lastDaySend = bb.findViewById(R.id.lastDaySend);
        Button lastDayExtract = bb.findViewById(R.id.lastDayExtract);
        Button lastWeekSend = bb.findViewById(R.id.lastWeekSend);
        Button lastWeekExtract = bb.findViewById(R.id.lastWeekExtract);
        Button lastMonthSend = bb.findViewById(R.id.lastMonthSend);
        Button lastMonthExtract = bb.findViewById(R.id.lastMonthExtract);
        builder.setView(bb);
        AlertDialog dialog = builder.create();
        dialog.show();

        lastDaySend.setOnClickListener(view -> {
            List<Test> testList = db.getLast(1);
            if (testList == null || testList.isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.no_last_day, Toast.LENGTH_LONG).show();
                return;
            }
            SharedPreferences p = getSharedPreferences(RandomUtils.KEY, 0);
            String doctorID = p.getString("doctorID", RandomUtils.EMPTY);
            String userName = p.getString("userName", RandomUtils.EMPTY);
            if (doctorID.equals(RandomUtils.EMPTY)) {
                Toast.makeText(MainActivity.this, R.string.no_doctor, Toast.LENGTH_LONG).show();
                return;
            }
            sendXls(testList, userName, doctorID);
            dialog.dismiss();
        });
        lastDayExtract.setOnClickListener(view -> {
            List<Test> testList = db.getLast(1);
            if (testList == null || testList.isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.no_last_day, Toast.LENGTH_LONG).show();
                return;
            }
            SharedPreferences p = getSharedPreferences(RandomUtils.KEY, 0);
            String userName = p.getString("userName", "No Name");
            createXls(false, testList, userName);
            Toast.makeText(MainActivity.this, R.string.saved_download, Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });

        lastWeekSend.setOnClickListener(view -> {
            List<Test> testList = db.getLast(7);
            if (testList == null || testList.isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.no_last_week, Toast.LENGTH_LONG).show();
                return;
            }
            SharedPreferences p = getSharedPreferences(RandomUtils.KEY, 0);
            String doctorID = p.getString("doctorID", RandomUtils.EMPTY);
            String userName = p.getString("userName", RandomUtils.EMPTY);
            if (doctorID.equals(RandomUtils.EMPTY)) {
                Toast.makeText(MainActivity.this, R.string.no_doctor, Toast.LENGTH_LONG).show();
                return;
            }
            sendXls(testList, userName, doctorID);
            dialog.dismiss();
        });
        lastWeekExtract.setOnClickListener(view -> {
            List<Test> testList = db.getLast(7);
            if (testList == null || testList.isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.no_last_week, Toast.LENGTH_LONG).show();
                return;
            }
            SharedPreferences p = getSharedPreferences(RandomUtils.KEY, 0);
            String userName = p.getString("userName", "No Name");
            createXls(false, testList, userName);
            Toast.makeText(MainActivity.this, R.string.saved_download, Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });

        lastMonthSend.setOnClickListener(view -> {
            List<Test> testList = db.getLast(30);
            if (testList == null || testList.isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.no_last_month, Toast.LENGTH_LONG).show();
                return;
            }
            SharedPreferences p = getSharedPreferences(RandomUtils.KEY, 0);
            String doctorID = p.getString("doctorID", RandomUtils.EMPTY);
            String userName = p.getString("userName", RandomUtils.EMPTY);
            if (doctorID.equals(RandomUtils.EMPTY)) {
                Toast.makeText(MainActivity.this, R.string.no_doctor, Toast.LENGTH_LONG).show();
                return;
            }
            sendXls(testList, userName, doctorID);
            dialog.dismiss();
        });
        lastMonthExtract.setOnClickListener(view -> {
            List<Test> testList = db.getLast(30);
            if (testList == null || testList.isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.no_last_month, Toast.LENGTH_LONG).show();
                return;
            }
            SharedPreferences p = getSharedPreferences(RandomUtils.KEY, 0);
            String userName = p.getString("userName", "No Name");
            createXls(false, testList, userName);
            Toast.makeText(MainActivity.this, R.string.saved_download, Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });
    }
    public void clickBloodTest(View v) {
        if (isConnected()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View bbb = inflater.inflate(R.layout.blood_test, null);
            viewHeartBPM = bbb.findViewById(R.id.viewHeartBPM);
            viewSpo2 = bbb.findViewById(R.id.viewSpo2);
            imgBeat = bbb.findViewById(R.id.imgBeat);
            Button btnSendDr = bbb.findViewById(R.id.btnSendDr);
            Button btnSaveTest = bbb.findViewById(R.id.btnSaveTest);
            builder.setView(bbb);
            AlertDialog dialog = builder.create();
            new Thread(serverListener).start();
            dialog.show();

            btnSendDr.setOnClickListener(view -> {
                if (viewHeartBPM.getText().toString().startsWith(RandomUtils.AVERAGE)) {
                    SharedPreferences p = getSharedPreferences(RandomUtils.KEY, 0);
                    String doctorID = p.getString("doctorID", RandomUtils.EMPTY);
                    String userName = p.getString("userName", RandomUtils.EMPTY);
                    if (!doctorID.equals(RandomUtils.EMPTY)) {
                        Calendar now = Calendar.getInstance();
                        int bpm = Integer.parseInt(viewHeartBPM.getText().toString().substring(9));
                        int spo2 = Integer.parseInt(viewSpo2.getText().toString().substring(9));
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder().url(new FormatCorrector().messageUrl(doctorID,userName,bpm,spo2,now)).build();
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                e.printStackTrace();
                                MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.save_no_send, Toast.LENGTH_LONG).show());
                            }
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                if (response.body() != null) {
                                    String myResponse = response.body().string();
                                    if (myResponse.charAt(6) == 't') {
                                        MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.saved_and_sent, Toast.LENGTH_LONG).show());
                                    } else {
                                        MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.save_no_send, Toast.LENGTH_LONG).show());
                                    }
                                } else {
                                    MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.save_no_send, Toast.LENGTH_LONG).show());
                                }
                            }
                        });
                        db.addTest(new Test(bpm, spo2, now.getTime()));
                        dbListUpdate();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(MainActivity.this, R.string.no_doctor, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.wait_average, Toast.LENGTH_SHORT).show();
                }
            });
            btnSaveTest.setOnClickListener(view -> {
                if (viewHeartBPM.getText().toString().startsWith(RandomUtils.AVERAGE)) {
                    Calendar now = Calendar.getInstance();
                    db.addTest(new Test(Integer.parseInt(viewHeartBPM.getText().toString().substring(9)),
                            Integer.parseInt(viewSpo2.getText().toString().substring(9)), now.getTime()));
                    Toast.makeText(MainActivity.this, R.string.saved, Toast.LENGTH_LONG).show();
                    dbListUpdate();
                    dialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, R.string.wait_average, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, R.string.you_not_connected, Toast.LENGTH_LONG).show();
        }
    }

    boolean isConnected() {
        return btSocket != null && btSocket.isConnected();
    }
    boolean isBluetoothEnabled(BluetoothAdapter btAdapter) {
        return btAdapter != null && btAdapter.isEnabled();
    }

    void viewConnect() {
        TextView isConnectText = findViewById(R.id.isConnectText);
        TextView btName = findViewById(R.id.btName);
        TextView btMAC = findViewById(R.id.btMAC);
        if (isConnected()) {
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            SharedPreferences p = getSharedPreferences(RandomUtils.KEY, 0);
            String MAC = p.getString("MAC", "00:00:00:00:00:00");
            BluetoothDevice btDevice = btAdapter.getRemoteDevice(MAC);

            isConnectText.setTextColor(ContextCompat.getColor(this, R.color.green));
            isConnectText.setText(R.string.connected);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { System.out.println("NOT_GRANTED"); }
            btName.setText(btDevice.getName());
            btMAC.setText(btDevice.getAddress());
        } else {
            isConnectText.setTextColor(ContextCompat.getColor(this, R.color.red));
            isConnectText.setText(R.string.not_connected);
            btName.setText(null);
            btMAC.setText(null);
        }
    }
    void viewBpmSpo2() {
        if (cVisit < 1) {
            cVisit++;
            return;
        }
        String fBPM = new FormatCorrector().filterBPM(serialStr);
        String fSpo2 = new FormatCorrector().filterSpo2(serialStr);
        if (fBPM != null && fSpo2 != null) {
            runOnUiThread(() -> {
                viewHeartBPM.setText(fBPM);
                viewSpo2.setText(fSpo2);
            });
        } else {
            String fAvgBPM = new FormatCorrector().filterAverageBPM(serialStr);
            String fAvgSpo2 = new FormatCorrector().filterAverageSpo2(serialStr);
            if (fAvgBPM != null && fAvgSpo2 != null) {
                runOnUiThread(() -> {
                    String showAvgBPM = RandomUtils.AVERAGE + fAvgBPM;
                    String showAvgSpo2 = RandomUtils.AVERAGE + fAvgSpo2;
                    viewHeartBPM.setText(showAvgBPM);
                    viewSpo2.setText(showAvgSpo2);
                });
            }
        }
    }
    void btDeviceConnect() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (isBluetoothEnabled(btAdapter)) {

            SharedPreferences p = getSharedPreferences(RandomUtils.KEY, 0);
            String MAC = p.getString("MAC", "00:00:00:00:00:00");
            BluetoothDevice btDevice = btAdapter.getRemoteDevice(MAC);
            btSocket = null;

            int counter = 0;
            do {
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { System.out.println("NOT_GRANTED"); }
                    btSocket = btDevice.createRfcommSocketToServiceRecord(RandomUtils.M_UUID);
                    btSocket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                counter++;
            } while (!btSocket.isConnected() && counter < 2);
        }
    }
    void dbListUpdate() {
        if (!db.isEmpty()) {
            dbTitleList.setVisibility(View.VISIBLE);
            dbList.setAdapter(new DbListAdapter(this));
            dbList.setOnItemClickListener((adapterView, view, i, l) -> {
                int newI = i + db.getCount();
                newI = newI - (i * 2);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View v = inflater.inflate(R.layout.show_send, null);
                TextView showUserName = v.findViewById(R.id.showUserName);
                TextView showBpm = v.findViewById(R.id.showBpm);
                TextView showSpo2 = v.findViewById(R.id.showSpo2);
                TextView showTime = v.findViewById(R.id.showTime);
                TextView showDate = v.findViewById(R.id.showDate);
                Button btnSendShowDr = v.findViewById(R.id.btnSendShowDr);
                Button btnCancelShow = v.findViewById(R.id.btnCancelShow);
                builder.setView(v);
                AlertDialog dialog = builder.create();
                SharedPreferences p = getSharedPreferences(RandomUtils.KEY, 0);
                String doctorID = p.getString("doctorID", RandomUtils.EMPTY);
                String userName = p.getString("userName", RandomUtils.EMPTY);
                if (!userName.equals(RandomUtils.EMPTY)) { showUserName.setText(userName); }
                String strBpm = "BPM: " + db.getTest(newI).getBpm();
                showBpm.setText(strBpm);
                String strSpo2 = db.getTest(newI).getSpo2() + " :Spo2";
                showSpo2.setText(strSpo2);
                Date d = db.getTest(newI).getDate();
                Calendar cal = Calendar.getInstance();
                cal.setTime(d);
                showTime.setText(new FormatCorrector().formatTime(cal));
                showDate.setText(new FormatCorrector().formatDate(cal));
                dialog.show();

                int finalNewI = newI;
                btnSendShowDr.setOnClickListener(view1 -> {
                    if (!doctorID.equals(RandomUtils.EMPTY)) {
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder().url(new FormatCorrector().messageUrl(doctorID,userName,db.getTest(finalNewI).getBpm(),db.getTest(finalNewI).getSpo2(),cal)).build();
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                e.printStackTrace();
                                MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.sorry_send, Toast.LENGTH_LONG).show());
                            }
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                if (response.body() != null) {
                                    String myResponse = response.body().string();
                                    if (myResponse.charAt(6) == 't') {
                                        MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.sent, Toast.LENGTH_LONG).show());
                                    } else {
                                        MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.sorry_send, Toast.LENGTH_LONG).show());
                                    }
                                } else {
                                    MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.sorry_send, Toast.LENGTH_LONG).show());
                                }
                            }
                        });
                        dialog.dismiss();
                    } else {
                        Toast.makeText(MainActivity.this, R.string.no_doctor, Toast.LENGTH_LONG).show();
                    }
                });
                btnCancelShow.setOnClickListener(view12 -> dialog.dismiss());
            });
        } else {
            dbTitleList.setVisibility(View.INVISIBLE);
        }
    }

    public final Runnable serverListener = new Runnable() {
        public void run() {
            try {
                InputStream is = btSocket.getInputStream();

                int bufferSize = 1024;
                int bytesRead;
                byte[] buffer = new byte[bufferSize];

                while(!STOP) {
                    System.out.println("g");
                    final StringBuilder sb = new StringBuilder();
                    bytesRead = is.read(buffer);
                    if (bytesRead != -1) {
                        StringBuilder result = new StringBuilder();
                        while ((bytesRead == bufferSize) && (buffer[bufferSize-1] != 0))
                        {
                            result.append(new String(buffer, 0, bytesRead - 1));
                            bytesRead = is.read(buffer);
                        }
                        result.append(new String(buffer, 0, bytesRead - 1));
                        sb.append(result);
                    }
                    serialStr = sb.toString();
                    viewBpmSpo2();
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    };
    public void sendXls(List<Test> testList, String userName, String doctorID) {
        createXls(true, testList, userName);
        JSONObject json = null;
        String jjj = "{\"chat_id\":\"" + doctorID + "\",\"document\":\"" +
                "content://com.android.externalstorage.documents/document/primary%3ADownload%2FBlood%20Flow%20App%2F.temp%2F" +
                tempFileName.replace(" ", "%20") + "\"}";
        try {
            json = new JSONObject(jjj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject finalJson = json;
        Thread thread = new Thread(() -> {
            try {
                MultipartUtility multipart = new MultipartUtility(MainActivity.this, RandomUtils.BOT_URL + RandomUtils.TOKEN + "/sendDocument");
                Iterator<String> keys = finalJson.keys();
                while (keys.hasNext()) {
                    final String key = keys.next();

                    try {
                        Object object = finalJson.get(key);
                        String value = object.toString();
                        if (value.startsWith("content://")) {
                            try {
                                Uri uri = Uri.parse(value);
                                String path = getPath(uri);
                                if (path != null) {
                                    File file = new File(path);
                                    multipart.addFilePart(key, file);
                                }
                            } catch (Exception e) {
                                Log.e("api", "file", e);
                                multipart.addFormField(key, value);
                            }
                        } else {
                            multipart.addFormField(key, value);
                        }
                    } catch (JSONException e) {
                        Log.e("api", "parsing", e);
                    }
                }
                final String res = multipart.finish();
                if (res.charAt(6) == 't') {
                    MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.sent, Toast.LENGTH_LONG).show());
                } else {
                    MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.no_send, Toast.LENGTH_LONG).show());
                }
            } catch (final Exception e) {
                Log.e("api", "send request", e);
                MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.no_send, Toast.LENGTH_LONG).show());
            }
        });
        thread.start();
    }
    public void createXls(boolean temp, List<Test> testList, String userName) {
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
        HSSFSheet hssfSheet = hssfWorkbook.createSheet(userName);
        HSSFRow hssfRow = hssfSheet.createRow(0);
        hssfRow.createCell(0).setCellValue("BPM");
        hssfRow.createCell(1).setCellValue("Spo2");
        hssfRow.createCell(2).setCellValue("Time");
        hssfRow.createCell(3).setCellValue("Date");

        int rowCounter = 0;
        for (Test m : testList) {
            rowCounter ++;
            HSSFRow row = hssfSheet.createRow(rowCounter);
            row.createCell(0).setCellValue(m.getBpm());
            row.createCell(1).setCellValue(m.getSpo2());
            Date d = m.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            row.createCell(2).setCellValue(new FormatCorrector().formatTime(cal));
            row.createCell(3).setCellValue(new FormatCorrector().formatDate(cal));
        }

        File root;
        File filePath;
        if (temp) {
            root = new File(RandomUtils.TEMP);
            tempFileName = userName + " " + onlyDate(testList.get(0).getDate()) + " to " + onlyDate(testList.get(rowCounter - 1).getDate()) + ".xls";
            filePath = new File(RandomUtils.TEMP + tempFileName);
        } else {
            root = new File(RandomUtils.FOLDER);
            filePath = new File(RandomUtils.FOLDER + userName + " " + onlyDate(testList.get(0).getDate()) + " to " + onlyDate(testList.get(rowCounter - 1).getDate()) + ".xls");
        }
        if (!root.exists()) { root.mkdirs(); }

        try {
            if (!filePath.exists()){
                filePath.createNewFile();
            }
            FileOutputStream fileOutputStream= new FileOutputStream(filePath);
            hssfWorkbook.write(fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String onlyDate(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return cal.get(Calendar.DAY_OF_MONTH) + "-" + (cal.get(Calendar.MONTH) + 1);
    }
    public String getPath(final Uri uri) {
        if (DocumentsContract.isDocumentUri(MainActivity.this, uri)) {   // DocumentProvider
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {   // ExternalStorageProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {   // DownloadsProvider

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                return getDataColumn(MainActivity.this, contentUri, null, null);
            } else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {   // MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(MainActivity.this, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {   // MediaStore (and general)

            // Return the remote address
            if ("com.google.android.apps.photos.content".equals(uri.getAuthority()))
                return uri.getLastPathSegment();

            return getDataColumn(MainActivity.this, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {   // File
            return uri.getPath();
        }

        return null;
    }
    public String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
    void deleteTemp(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles())) {
                deleteTemp(child);
            }
        }
        fileOrDirectory.delete();
    }
}