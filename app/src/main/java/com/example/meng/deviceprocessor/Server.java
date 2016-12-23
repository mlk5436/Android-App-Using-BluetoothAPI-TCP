package com.example.meng.deviceprocessor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.example.meng.deviceprocessor.MainActivity.mBluetoothAdapter;
import static com.example.meng.deviceprocessor.R.id.text;

public class Server extends AppCompatActivity {
    TextView serverstatus;
    InputStream mmInStream;
    OutputStream mmOutStream;
    Button beginListening;
    BluetoothSocket socket;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;
            String data = msg.getData().getString("message");
            if((null!=data)){
                serverstatus.append(data);
            }else{
                serverstatus.append("no data");
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
                    serverstatus.setText(text.toString());
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        serverstatus = (TextView) findViewById(R.id.serverS);
        beginListening = (Button) findViewById(R.id.beginListening);
        beginListening.setOnClickListener(new View.OnClickListener(){
            public void onClick(View V){
                AcceptThread mAcceptThread = new AcceptThread();
                mAcceptThread.start();
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
                serverstatus.append("\nDevice connected!");
                //textView.setText("Device connected! ");
            }
            else if (MainActivity.mBluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected
                serverstatus.append("\nDevice disconnected!");


            }
        }
    };

    private class AcceptThread extends Thread{
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try{
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Meng", MainActivity.MY_UUID);
            }catch(IOException e){
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }

        public void run(){
            Message msgObj = mHandler.obtainMessage();
            Bundle b = new Bundle();
            socket = null;
            while(true) {
                try {
                    String msg = "\nListening for connection";
                    b.putString("message", msg);
                    msgObj.setData(b);
                    mHandler.sendMessage(msgObj);
                    socket = mmServerSocket.accept();

                } catch (IOException e) {

                    e.printStackTrace();
                    break;
                }
                if (socket != null) {
                    Thread mConnected = new ConnectedThread(socket);
                    mConnected.start();
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                    }

                    break;
                }
            }
        }

    }

    public class ConnectedThread extends Thread{
        public ConnectedThread(BluetoothSocket Socket){
            socket = Socket;
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
                    if(buffer != null){
                        String data = new String(buffer, 0, bytes);
                        Message msgObj = mHandler.obtainMessage();
                        Bundle b = new Bundle();
                        String msg= " \nData: " +data;
                        b.putString("message", msg);
                        msgObj.setData(b);
                        mHandler.sendMessage(msgObj);
                    }else{
                        Message msgObj = mHandler.obtainMessage();
                        Bundle b = new Bundle();
                        String msg= "no Data!";
                        b.putString("message", msg);
                        msgObj.setData(b);
                        mHandler.sendMessage(msgObj);
                    }
                   // mHandler.obtainMessage(1, bytes, -1, buffer).sendToTarget();

                }catch (IOException e){e.printStackTrace();}
                break;
            }
        }

    }
    public void cancel(){
        try{
            socket.close();
        }catch (IOException e){}
    }
}
