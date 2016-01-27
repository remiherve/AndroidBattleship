package fr.info.orleans.androidbattleship.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import fr.info.orleans.androidbattleship.R;

public class BluetoothActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    ArrayAdapter<String> listAdapter;
    ListView listView;
    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> devicesArray;
    ArrayList<String> pairedDevices;
    ArrayList<BluetoothDevice> devices;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    private String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";
    IntentFilter filter;
    BroadcastReceiver receiver;
    String tag = "debugging";
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Log.i(tag, "in handler");
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;

            switch(msg.what){
                case SUCCESS_CONNECT:
                    // DO something
                    ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
                    Toast.makeText(getApplicationContext(), "CONNECT", Toast.LENGTH_SHORT).show();
                    String s = "successfully connected";
                    connectedThread.write(s.getBytes());
                    Log.i(tag, "connected");
                    break;
                case MESSAGE_READ:

//                    byte[] readBuf = (byte[])msg.obj;
//                    String string = new String(readBuf);
//                    Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
//                    break;
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    break;
            }
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        init();
        if(btAdapter==null){
            Toast.makeText(getApplicationContext(), "No bluetooth detected", Toast.LENGTH_SHORT).show();
            finish();
        }
        else{
            if(!btAdapter.isEnabled()){
                turnOnBT();
            }

            getPairedDevices();
            startDiscovery();
        }


    }
    private void startDiscovery() {
        btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();

    }
    private void turnOnBT() {
        Intent intent =new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, 1);
    }
    private void getPairedDevices() {
        devicesArray = btAdapter.getBondedDevices();
        if(devicesArray.size()>0){
            for(BluetoothDevice device:devicesArray){
                pairedDevices.add(device.getName());

            }
        }
    }
    private void init() {
        listView= (ListView) findViewById(R.id.listViewBluetoothDevices);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        listAdapter= new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, 0);
        listView.setAdapter(listAdapter);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = new ArrayList<>();
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        devices = new ArrayList<BluetoothDevice>();
        receiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(device);
                    String s = "";
                    for(int a = 0; a < pairedDevices.size(); a++){
                        if(device.getName().equals(pairedDevices.get(a))){
                            //append
                            s = "(Paired)";
                            break;
                        }
                    }

                    listAdapter.add(device.getName()+" "+s+" "+"\n"+device.getAddress());
                }

                else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                    // run some code
                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    // run some code



                }
                else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                    if(btAdapter.getState() == btAdapter.STATE_OFF){
                        turnOnBT();
                    }
                }

            }
        };

        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_CANCELED){
            Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                            long arg3) {

        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }

        BluetoothDevice selectedDevice = devices.get(arg2);

        if(listAdapter.getItem(arg2).contains("Paired")){

            AcceptThread acceptThread = new AcceptThread();
            ConnectThread connect = new ConnectThread(selectedDevice);
            connect.start();

            Log.i(tag, "in click listener");
        }
        else{

            pairDevice(selectedDevice);


        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothDevice selectedDevice = devices.get(position);
        unpairDevice(selectedDevice);
        //ces deux lignes refresh l'activité, OK pour unPair
        finish();
        startActivity(getIntent());
        return true;
    }


    private void pairDevice(BluetoothDevice device) {
        try {
            Log.d("pairDevice()", "Start Pairing...");
            Intent intentBluetooth = new Intent();
            intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intentBluetooth);
            Log.d("pairDevice()", "Pairing finished.");
        } catch (Exception e) {
            Log.e("pairDevice()", e.getMessage());
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Log.d("unpairDevice()", "Start Un-Pairing...");
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            Log.d("unpairDevice()", "Un-Pairing finished.");
        } catch (Exception e) {
            Log.e("unpairDevice()", e.getMessage());
        }
    }



    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = btAdapter.listenUsingRfcommWithServiceRecord("Server", MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        private void manageConnectedSocket(BluetoothSocket socket) {
            Log.i(tag,"AcceptThread.manageConnectedSocket");
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }



    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            Log.i(tag, "construct");
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.i(tag, "get socket failed");

            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();
            Log.i(tag, "connect - run");
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Log.i(tag, "connect - succeeded");
            } catch (IOException connectException) {
                Log.i(tag, "connect failed");
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            ConnectedThread mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }



        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];;  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            int begin = 0;

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes += mmInStream.read(buffer,bytes,buffer.length - bytes);
                    // Send the obtained bytes to the UI activity
                    for(int i = begin ; i < bytes ; i++){
                        if(buffer[i] == "#".getBytes()[0]){
                            mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                    .sendToTarget();
                            begin = i + 1;
                            if(i == bytes - 1){
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
