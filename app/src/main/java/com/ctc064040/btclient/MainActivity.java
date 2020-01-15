package com.ctc064040.btclient;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private BluetoothAdapter mBluetoothAdapter;
    final int REQUEST_ENABLE_BT = 1;
    ListView listView;
    TextView textView_DeviceName;
    TextView textView_DeviceAddress;
    TextView textView_Status;
    ImageView imageView1;
    ArrayList<BluetoothDevice> mBluetoothDeviceList;
    ArrayList<String> mDeviceNameList;
    ArrayAdapter<String> mArrayAdapter;

    BTClientThread btClientThread;
    BluetoothDevice mServerBluetoothDevice;

    Handler mUiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView_Status = findViewById(R.id.textView_Status);
        textView_Status.setText("Status:");

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.d(MainActivity.class.getName(), "Device does not support Bluetooth");
            textView_Status.setText("Status: Device does not support Bluetooth.");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        textView_DeviceName = findViewById(R.id.textView_DeviceName);
        textView_DeviceName.setText("Nothing");

        textView_DeviceAddress = findViewById(R.id.textView_DeviceAddress);
        textView_DeviceAddress.setText("00:00:00:00:00:00");

        mBluetoothDeviceList = new ArrayList<>();

        listView = findViewById(R.id.ListView1);
        mDeviceNameList = new ArrayList<>();
        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDeviceNameList);
        listView.setAdapter(mArrayAdapter);
        listView.setOnItemClickListener(this);

        imageView1 = findViewById(R.id.imageView1);

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : pairedDevices) {
                String nameAndAddress = bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress();
                mArrayAdapter.add(nameAndAddress);
                mBluetoothDeviceList.add(bluetoothDevice);
            }
        }

        Button b = findViewById(R.id.Button_Listen);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btClientThread != null){
                    btClientThread.sendTakePictureCommand();
                }
            }
        });

        Button connectButton = findViewById(R.id.Button_Connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mServerBluetoothDevice != null) {
                    btClientThread = new BTClientThread();
                    btClientThread.start();
                }
            }
        });

        Button bi = findViewById(R.id.Button_Image);
        bi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btClientThread != null){
                    btClientThread.sendGetImageCommand();
                }
            }
        });

        mUiHandler = new Handler(Looper.getMainLooper());

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "Bluetooth を使用できません。", Toast.LENGTH_LONG).show();
                    // finish();
                    return;
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //String msg = position + "番目のアイテムがクリックされました";
        //Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        BluetoothDevice btDev = mBluetoothDeviceList.get(position);
        textView_DeviceName.setText(btDev.getName());
        textView_DeviceAddress.setText(btDev.getAddress());
        mServerBluetoothDevice = btDev;
    }

    public class BTClientThread extends Thread {
        static final String TAG = "BTTest1Client";
        UUID BT_UUID = UUID.fromString(
                "41eb5f39-6c3a-4067-8bb9-bad64e6e0908");
        InputStream inputStream;
        OutputStream outputStream;
        BluetoothSocket bluetoothSocket;

        public void run() {
            byte[] incomingBuff = new byte[64];
            BluetoothDevice bluetoothDevice = mServerBluetoothDevice;
            if(bluetoothDevice == null){
                Log.d(TAG, "No device found.");
                setStatusTextView("Status: No device found.");
                return;
            }

            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
                while(true) {
                    if(Thread.interrupted()){
                        break;
                    }

                    try {
                        bluetoothSocket.connect();
                        setStatusTextView("Status: サーバに接続しました。");

                        inputStream = bluetoothSocket.getInputStream();
                        outputStream = bluetoothSocket.getOutputStream();

                        while (true) {
                            if (Thread.interrupted()) {
                                break;
                            }

                            // Update again in a few seconds
                            Thread.sleep(3000);
                        }

                    } catch (IOException e) {
                        // connect will throw IOException immediately
                        // when it's disconnected.
                        Log.d(TAG, e.getMessage());
                    }

                    setStatusTextView("Status: サーバから切断されました。");

                    // Re-try after 3 sec
                    Thread.sleep(3 * 1000);
                }

            }catch (InterruptedException e){
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            if(bluetoothSocket != null){
                try {
                    bluetoothSocket.close();
                } catch (IOException e) {}
                bluetoothSocket = null;
            }
        }

        void sendTakePictureCommand(){
            try {
                if (outputStream != null) {
                    // Send Command
                    String command = "TakePicture";
                    outputStream.write(command.getBytes());
                }
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }
        }

        void sendGetImageCommand(){
            try {
                if (outputStream != null) {
                    // Send Command
                    String command = "GetImage";
                    outputStream.write(command.getBytes());
                    byte[] b = new byte[1024];
                    int incomingBytes = inputStream.read(b);
                    if(incomingBytes != 0) {
                        setStatusTextView("テスト成功");
                        // サーバからビットマップイメージが送られてきたら変換して表示
                        try {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(b,0,incomingBytes);
                            imageView1.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //imageView1.setImageResource(R.drawable.ic_launcher_background);
                    }
                }
                //try {
                //    android.content.res.Resources r = getResources();
                //    Bitmap bitmap = BitmapFactory.decodeResource(r,R.drawable.sankaku);
                //    imageView1.setImageBitmap(bitmap);
                //} catch (Exception e) {
                //    e.printStackTrace();
                //}

            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }
        }

        private void setStatusTextView(final String str){
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    textView_Status.setText(str);
                }
            });
        }
    }
}
