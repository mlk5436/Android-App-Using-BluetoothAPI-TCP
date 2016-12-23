package com.example.meng.deviceprocessor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.renderscript.ScriptGroup;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import static com.example.meng.deviceprocessor.MainActivity.mBluetoothAdapter;
import static com.example.meng.deviceprocessor.MainActivity.mmDevice;

public class Client extends AppCompatActivity {
    ArrayAdapter<String> adapter;
    ListView listView;
    InputStream mmInStream;
    OutputStream mmOutStream;
    Button send;
    TextView textview;
    TextView datastatus;
    EditText X, Y, Z;
    TextView inputData;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    Button closeConnection;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;
            String data = msg.getData().getString("message");
            if((null!=data)){
                textview.append(data);
            }else{
                textview.append("no data");
            }
            switch(msg.what) {
                case 1:
                    //data = (String) msg.obj;
                    File input = (File)msg.obj;
                    StringBuilder text = new StringBuilder();
                    try{
                        // FileInputStream fiStream = new FileInputStream(inputFile);
                        BufferedReader br = new BufferedReader(new FileReader(input));
                        String line;
                        while((line=br.readLine())!= null){
                            text.append(line);
                            text.append('\n');
                        }
                        br.close();
                    }catch (IOException e){e.printStackTrace();}
                    textview.setText(text.toString());
                    break;

            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        X = (EditText) findViewById(R.id.x);
        Y = (EditText) findViewById(R.id.y);
        Z = (EditText) findViewById(R.id.z);
        X.setVisibility(View.INVISIBLE);
        Y.setVisibility(View.INVISIBLE);
        Z.setVisibility(View.INVISIBLE);
        datastatus = (TextView) findViewById(R.id.dataStatus);
        datastatus.setVisibility(View.INVISIBLE);
        inputData = (TextView) findViewById(R.id.InputData);
        inputData.setVisibility(View.INVISIBLE);

        closeConnection = (Button) findViewById(R.id.closeSocket);
        closeConnection.setVisibility(View.INVISIBLE);
        textview = (TextView) findViewById(R.id.clientStatus);
        send = (Button) findViewById(R.id.send);
        send.setVisibility(View.INVISIBLE);
        listView = (ListView) findViewById(R.id.listview);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) { // make listview clickable to connect to particular device
                //MainActivity.mBluetoothAdapter.cancelDiscovery();
                Set<BluetoothDevice> pairedDevices = MainActivity.mBluetoothAdapter.getBondedDevices();
                String info = ((TextView) view).getText().toString();
                int i = 0;
                for (BluetoothDevice listedDevice : pairedDevices) {
                    if (i == position) {
                        MainActivity.mmDevice = listedDevice;
                        adapter.clear();
                        adapter.add("\nDevice: " + MainActivity.mmDevice.getName() + " " + MainActivity.mmDevice.getAddress() + "\n");
                        final Thread mConnectThread = new ConnectThread(MainActivity.mmDevice);
                        mConnectThread.start();

                    }
                    i++;
                }
            }
        });

        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);
        this.registerReceiver(mReceiver, filter3);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                textview.setText(" Device connected!");
                closeConnection.setVisibility(View.VISIBLE);
                closeConnection.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        textview.setText("Attempting to Disconnect");
                        cancel();
                    }
                });
                X.setVisibility(View.VISIBLE);
                Y.setVisibility(View.VISIBLE);
                Z.setVisibility(View.VISIBLE);
                inputData.setVisibility(View.VISIBLE);
                send.setVisibility(View.VISIBLE);
                datastatus.setVisibility(View.VISIBLE);
                send.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        datastatus.setText("sending Data");
                        byte[] xin = X.getText().toString().getBytes();
                        byte[] yin = Y.getText().toString().getBytes();
                        byte[] zin = Z.getText().toString().getBytes();
                        write(xin);
                        write(yin);
                        write(zin);
                    }
                });
            }
            else if (MainActivity.mBluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected
                textview.setText(" Device disconnected!");

                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if(pairedDevices.size() > 0){
                    adapter.clear();
                    for(BluetoothDevice devices : pairedDevices){
                        // textView.setText("Bluetooth Device Found");
                        adapter.add("\nDevice: " + devices.getName() + " " + devices.getAddress() + "\n");

                    }
                }
                closeConnection.setVisibility(View.INVISIBLE);
                X.setVisibility(View.INVISIBLE);
                Y.setVisibility(View.INVISIBLE);
                Z.setVisibility(View.INVISIBLE);
                inputData.setVisibility(View.INVISIBLE);
                send.setVisibility(View.INVISIBLE);
                datastatus.setVisibility(View.INVISIBLE);
            }
        }
    };


    private class ConnectThread extends Thread {

        public ConnectThread(BluetoothDevice device){
            BluetoothSocket tmp = null;
            mmDevice = device;

            try{
                tmp = mmDevice.createRfcommSocketToServiceRecord(MainActivity.MY_UUID);

            }catch (IOException e){e.printStackTrace();}
            mmSocket = tmp;
        }

        public void run(){
            mBluetoothAdapter.cancelDiscovery();
            Message msgObj = mHandler.obtainMessage();
            Bundle b = new Bundle();
            try{
                String msg= " Attempting to connect";
                b.putString("message", msg);
                msgObj.setData(b);
                mHandler.sendMessage(msgObj);
                mmSocket.connect();

            }catch (IOException e){
                try{
                    mmSocket.close();
                }catch (IOException e1){e1.printStackTrace();}
                return;
            }
            Thread mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }


    }
    public class ConnectedThread extends Thread{
        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }catch (IOException e){e.printStackTrace();}
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            while(true){
                try{
                    bytes = mmInStream.read(buffer);
                    mHandler.obtainMessage(1, bytes, -1, buffer).sendToTarget();
                }catch (IOException e){e.printStackTrace();}
                break;
            }
        }

    }
    public void write(byte[] bytes){
        try{
            mmOutStream.write(bytes);
            datastatus.setText("Data sent");
        }catch(IOException e){e.printStackTrace();
            datastatus.setText("Data not sent");
        }
    }
    public void cancel(){
        try{
            mmSocket.close();
        }catch (IOException e){}
    }
    @Override
    public void onStart(){
        super.onStart();
        if(!mBluetoothAdapter.isEnabled()){
            textview.setText(" Bluetooth disabled!");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, MainActivity.REQUEST_ENABLE_BT);
        }else{
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if(pairedDevices.size() > 0){
                adapter.clear();
                for(BluetoothDevice device : pairedDevices){
                    // textView.setText("Bluetooth Device Found");
                    adapter.add("\nDevice: " + device.getName() + " " + device.getAddress() + "\n");

                }
            }
        }
    }
}
