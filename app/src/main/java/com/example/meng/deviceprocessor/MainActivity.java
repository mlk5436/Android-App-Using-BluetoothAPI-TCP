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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothDevice mmDevice;
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int REQUEST_ENABLE_BT =1;
    TextView status;
    public static boolean connection;
    Button Client;
    Button Server;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        status = (TextView) findViewById(R.id.textview);

        Client = (Button) findViewById(R.id.clientID);
        Server = (Button) findViewById(R.id.serverID);

        Client.setOnClickListener(new View.OnClickListener(){
            public void onClick(View V){
                Intent intent = new Intent(MainActivity.this, Client.class );
                MainActivity.this.startActivity(intent);
            }
        });

        Server.setOnClickListener(new View.OnClickListener(){
            public void onClick(View V){
                Intent intent = new Intent(MainActivity.this, Server.class );
                MainActivity.this.startActivity(intent);
            }
        });


    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){
            }

        }
    }
    @Override
    public void onStart(){
        super.onStart();
        if(mBluetoothAdapter== null){
            status.setText("Device does not support Bluetooth");
        }else{
            if(!mBluetoothAdapter.isEnabled()){
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mBluetoothAdapter.cancelDiscovery();
    }
}
