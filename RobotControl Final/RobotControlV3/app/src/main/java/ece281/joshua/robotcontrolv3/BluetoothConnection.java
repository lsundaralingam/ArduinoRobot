package ece281.joshua.robotcontrolv3;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class contains code for creating and managing bluetooth connections. This is based off that from Google's documentation/tutorial found at:
 * http://developer.android.com/guide/topics/connectivity/bluetooth.html
 *
 */
public class BluetoothConnection {

    BluetoothAdapter mAdapter;//Represents the phone's bluetooth device
    BluetoothSocket mSocket;//Socket that wil connect to the arduino bluetooth module

    Handler messageHandler; //Handler for precessing data from the arduino

    private final int RECIEVE_MESSAGE = 1; //Flag used to specify that a message has been received from the arduio
    private final int REQUEST_ENABLE_BT = 1;//Used for the creation of a popup dialog

    private final String TAG = "BLUETOOTH CONNECTION"; //Tag for logging/debugging purposes

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-0180511004FB"); //Arbitrary string to identify app
    Activity parentActivity;//Reference to the parent activity that creates this object(MainActivity)

    ConnectionThread connectionThread;

    boolean bluetoothConnected;//If we are connected to the arduino bluetooth module

    /*
    Constructor for BluetoothConnection takes references to the parentActivity so that the Toast widget can be used, and the message Handler
    so that data from the arduino can be processed outside of the ConnectionThread used in this class.
     */
    public BluetoothConnection(Activity parentActivity, Handler messageHandler){
        this.parentActivity = parentActivity;
        this.messageHandler = messageHandler;
    }


    /*
    Begins the process of connecting to a bluetooth device. Creates a new BluetoothAdapter which represents the local bluetooth device.
    Also checks to see if bluetooth is enabled, if not, an option dialog is created prompting the user to turn on bluetooth
     */
    public void configureBluetooth(){

        mAdapter = BluetoothAdapter.getDefaultAdapter(); //Sets mAdapter to the default local bluetooth device
        //If bluetooth is not enabled
        if(!mAdapter.isEnabled()){
            //Creates a popup dialog box that prompts the user to turn on bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            parentActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    /*
    Creates a new bluetooth device that represents the remote(arduino) bluetooth module and attempts to form a connection with it
    The parameter address is the MAC address of the bluetooth module
     */
    public void connectToBluetoothDevice(String address){

        //Initializes the bluetooth device object based on the given MAC adress
        BluetoothDevice mDevice = mAdapter.getRemoteDevice(address);

        //Attempts to create a new BluetoothSocket that will allow for the exchange of data between the bluetooth devices
        //My_UUID is an application unique string that identifies the application
        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Toast.makeText(parentActivity.getBaseContext(), "Could not create socket", Toast.LENGTH_LONG).show();
        }

        //Ends the process of device discovery which consumes a large amount of processing power
        mAdapter.cancelDiscovery();

        //Connects the socket
        try {
            mSocket.connect();
            Toast.makeText(parentActivity.getBaseContext(), "Connected Successfully", Toast.LENGTH_SHORT).show();
            bluetoothConnected = true;
        } catch (IOException e) {
            try {//If we cannot connect, display an error message to the user
                mSocket.close();
                Toast.makeText(parentActivity.getBaseContext(), "Could not connect to device", Toast.LENGTH_LONG).show();
                bluetoothConnected = false;
            } catch (IOException e2) { //If we cannot connect and cannot abort
                Log.d(TAG, "huh");
            }
        }

        //Creates and starts a new thread that will manage transmission/reception of data between the app and the arduino
        connectionThread = new ConnectionThread(mSocket);
        connectionThread.start();
    }

    /*
    Closes the socket connection and sets the bluetoothConnected flag to false
     */
    public void closeSocket(){
        try     {
            mSocket.close();
            bluetoothConnected = false;
        } catch (IOException e2) {//socket failed to close
            Log.d(TAG, "Could not close Socket");
        }
    }

    /*
    Method is called from MainActivity when there is a new command to be sent to the arduino
     */
    public void sendData(String data){
        connectionThread.write(data); //writes the data through connectionThread
    }

    /*
    Returns whether we are currently connected to the arduino bluetooth module
     */
    public boolean isConnected(){
        return bluetoothConnected;
    }

    /*
    ConnectionThread listens for incoming data from the arduino and can also write data to arduino
     */
    private class ConnectionThread extends Thread{

        private final InputStream mmInStream; //Stream for data from arduino
        private final OutputStream mmOutStream;//Stream for data to be sent to arduino

        public ConnectionThread(BluetoothSocket socket) {
            //Temporary input and output stream objects.  InputStream and OutputStream objects are final so we need to use temporary objects in case getInputStream() or
            //getOutputStream() fail
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //Trys to create InputStream and OutputStream
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        //This method handles input from the arduino bluetooth module. It reads a stream of bytes into a buffer, which is sent to the message handler
        public void run() {
            byte[] buffer = new byte[256];  //buffer for the input stream.
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    messageHandler.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        //This method is called whenever there is data/commands to be sent to the arduino
        public void write(String message) {
            byte[] msgBuffer = message.getBytes(); //The message to be sent as a sequence of bytes
            try {
                mmOutStream.write(msgBuffer);//Writes the output buffer to the bluetooth socket's output stream
            } catch (IOException e) {
                //Displays an error message to the user through the Toast widget
               // Toast.makeText(parentActivity.getBaseContext(), "Error writing to device", Toast.LENGTH_LONG).show();
            }
        }
    }
}
