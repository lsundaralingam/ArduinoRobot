package ece281.joshua.robotcontrolv3;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity implements SensorEventListener{


    //Constants for different input/status modes
    final int AUTO_MODE = 0;
    final int BUTTON_MODE = 1;
    final int ACCELEROMETER_MODE = 2;
    final int IDLE_MODE = 3;
    //Which mode the robot is currently in
    int mode;

    final int BLUETOOTH_COM_INTERVAL = 100; //Interval, in milliseconds at which the app sends and receives data from the arduino

    //Default speed for travelling forwards or backwards in a straight line. It is 238 and not 255 to account for scaling done by the arduino
    //in order to equalize the two motors so the robot travels in a straight line.
    int defaultSpeed = 238;

    //Direction flags controlled by the four buttons in Button Control Mode
    boolean left, right, forward, reverse;

    private final int RECIEVE_MESSAGE = 1;

    // MAC-address of Bluetooth module.
    private static String address = "30:14:11:14:09:19";

    //Tag used for log
    private static final String TAG = "Robot Control V3";

    //Integers representing the power of the two motors
    int speedLeft, speedRight, xSpeed;
    Timer updateTimer;

    //SensorManager object for the accelerometer
    private SensorManager mSensorManager;
    //Sensor object for the accelerometer
    private Sensor mAccelerometer;


    //DisplayScreen objects for each of the three ui/control modes
    DisplayScreen buttonControlDisplayScreen;
    DisplayScreen autoControlDisplayScreen;
    DisplayScreen accelControlDisplayScreen;

    //BluetoothConnection object. The BluetoothConnection class contains code to create and manage a bluetooth connection
    BluetoothConnection bluetoothConnection;
    StringBuilder sb;

    /*
    The onCreate method is called automatically when the app is first started. It initializes the UI, accelerometer sensor, bluetoothConnection as well
    as a timer used to transmit commands through bluetooth at a specific interval.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mode = BUTTON_MODE; //initializes mode to button mode
        initializeButtonUI();
        sb = new StringBuilder();

        //Sets up the accelerometer sensor
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Creates a new bluetoothConnection object
        bluetoothConnection = new BluetoothConnection(this, mHandler);

        //configureBluetooth() will turn the phone's bluetooth on if not on already
        bluetoothConnection.configureBluetooth();

        //Creates a new timer that will communicate with the arduino at a set interval
        updateTimer = new Timer();
        CommunicationTimerTask comTimer = new CommunicationTimerTask(); //TimerTask that executes is executed at the set interval
        updateTimer.schedule(comTimer, BLUETOOTH_COM_INTERVAL, BLUETOOTH_COM_INTERVAL);//schedules the CommunicationTimerTask
    }

    /*
    The onResume method is called automatically shortly after onCreate. It registers the accelerometer sensor so that onSensorChanged
    can be used to track changes in the accelerometer, and connects to the bluetooth module
     */

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);//Listens for changes in the accelerometer

        //Supplies bluetoothConnection with the bluetooth module's MAC adresses. A connection socket and thread will be created.
        bluetoothConnection.connectToBluetoothDevice(address);
    }

    /*
    Initializes the Button Control UI
     */
    public void initializeButtonUI(){

        setContentView(R.layout.button_control_layout); //Use button_control_layout.xml for the UI layout
        buttonControlDisplayScreen = (DisplayScreen)findViewById(R.id.displayScreen);

        //Resets motor values
        speedLeft = 0;
        speedRight = 0;


        /*
        Sets up onTouchListers for each of the four direction buttons.  The onTouch method will be called whenever the user
        presses or lifts off the button and will set the appropriate direction stat flag to true or false/
         */

        Button forwardButton = (Button) findViewById(R.id.forwards_button);
        forwardButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionevent) {
                int action = motionevent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    forward = true;
                } else if (action == MotionEvent.ACTION_UP) {
                    forward = false;
                }
                return false;
            }
        });

        Button reverseButton = (Button) findViewById(R.id.reverse_button);
        reverseButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionevent) {
                int action = motionevent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    reverse = true;
                } else if (action == MotionEvent.ACTION_UP) {
                    reverse = false;
                }
                return false;
            }
        });

        Button rightButton = (Button) findViewById(R.id.right_button);
        rightButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionevent) {
                int action = motionevent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    right = true;
                } else if (action == MotionEvent.ACTION_UP) {
                    right = false;                }
                return false;
            }
        });


        Button leftButton = (Button) findViewById(R.id.left_button);
        leftButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionevent) {
                int action = motionevent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    left = true;
                } else if (action == MotionEvent.ACTION_UP) {
                    left = false;                }
                return false;
            }
        });
    }

    /*
    Initializes the Accelerometer Control UI
     */
    public void initializeAccelUI(){
        setContentView(R.layout.accel_control_layout);//Use accel_control_layout.xml for the UI layout
        accelControlDisplayScreen =(DisplayScreen)findViewById(R.id.displayScreenAccel);
    }

    public void initializeAutoUI(){
        setContentView(R.layout.auto_control_layout);//Use auto_control_layout.xml for the UI layout
        autoControlDisplayScreen = (DisplayScreen)findViewById(R.id.displayScreenAuto);
        mode = IDLE_MODE; //Set mode to idle so the robot will do nothing until the start button is pressed
    }

    //Called when the start button in automatic mode is pressed.
    //Sets the current mode to AUTO_MODE, so the robot will begin acting autonomously
    public void startButtonClick(View view){
        mode = AUTO_MODE;
    }

    //Called when the stop button in automatic mode is pressed.
    //Sets the current mode to IDLE_MODE, so the robot will stop and wait for instructions
    public void stopButtonClick(View view){
        mode = IDLE_MODE;
    }

    //Determines the appropriate command to send to the arduino based on current state flags
    //and sends that command to the bluetoothConnection module to be transmitted
    public void sendData(){

        char stateFlag; //Character that represents the next state/mode of the robot

        //If we are currently in the button control mode, set the speed of each motor based on the direction flags
        if(mode == BUTTON_MODE){
            if(forward){
                setSpeed(defaultSpeed,defaultSpeed);
            }else if(reverse){
                setSpeed(-defaultSpeed,-defaultSpeed);
            }else if(right){
                setSpeed(defaultSpeed,-defaultSpeed);
            }else if(left){
                setSpeed(-defaultSpeed,defaultSpeed);
            }else{
                setSpeed(0,0);
            }
            stateFlag = 'm'; //Set the stateFlag to 'm' for 'manual'
        }
        //If we are currently in accelerometer control mode, each motor speed value will be determined by the accelerometer
        //orientation
        else if(mode == ACCELEROMETER_MODE){
            stateFlag = 'm';//Set the stateFlag to 'm' for 'manual'
        }
        //If we are currently in automatic mode, set motor speed values to 0 (arbitrary) and set stateFlag to 'a' for 'automatic'
        else if(mode == AUTO_MODE){
            setSpeed(0,0);
            stateFlag = 'a';
        }
        //If we are not in button, acceleromenter, or automatic mode, then we are in idle mode
        else{
            setSpeed(0,0); //Stop motors
            stateFlag = 'i'; //set the stateFlag to 'i' for 'idle'
        }

        //toSend is a 3 part string containing:
        //  1. The stateFlag
        //  2. The left motor speed followed by an 'n' character to seperate it from the
        //  3. Right motor speed
        String toSend = stateFlag + Integer.toString(speedLeft);
        toSend += "n"+ Integer.toString(speedRight);

        Log.d(TAG, toSend);

        bluetoothConnection.sendData(toSend); //Calls the sendData method in the bluetoothConnection object that will transmit the command
    }

    /*
    Sets the speed of the left and right motor
     */
    public void setSpeed(int left, int right){

        //If the speeds are not between -250, pwm on the arduino will not work. This should never happen though
        if(left >= 255 || left <= -255 || right >= 255 || right <= -255){
            Toast.makeText(getBaseContext(), "Invalid speed set, stopping motors", Toast.LENGTH_LONG).show();
            speedLeft = 0;
            speedRight = 0;
        }else{
            speedLeft = left;
            speedRight = right;
        }
    }

    //Will update the display based on a new distance read from the ultrasonic sensor
   public void updateDisplay(int distance){

       //Checks the current mode and updates the appropriate displayScreen object
        if(mode == AUTO_MODE || mode == IDLE_MODE){
            autoControlDisplayScreen.updateDisplay(distance);
        }else if(mode == BUTTON_MODE){
            buttonControlDisplayScreen.updateDisplay(distance);
        }else if(mode == ACCELEROMETER_MODE){
            accelControlDisplayScreen.updateDisplay(distance);
        }
    }

    //Changes the UI based on the current mode
    public void changeUI(){
        if(mode == AUTO_MODE){
            initializeAutoUI();
        }else if(mode ==BUTTON_MODE){
            initializeButtonUI();
        }else if(mode == ACCELEROMETER_MODE){
            initializeAccelUI();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /*
    Handles the option menu to select modes, as well as reconnect bluetooth in case the connection is lost
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();//Which menu item was selected

        switch (id) {
            case R.id.button_mode_select: //If button Mode was selected change mode to button mode and change UI
                mode = BUTTON_MODE;
                changeUI();
                return true;
            case R.id.auto_mode_select: //If AutoMode was selected change mode to auto mode and change UI
                mode = AUTO_MODE;
                changeUI();
                return true;
            case R.id.accel_mode_select: //If button Mode was selected change mode to accelerometer mode and change UI
                mode = ACCELEROMETER_MODE;
                changeUI();
                return true;
            case R.id.reconnect_bluetooth: //If reconnect bluetooth was selected
               if(!bluetoothConnection.isConnected()){//Check to see if we have lost connection
                    bluetoothConnection.connectToBluetoothDevice(address);//If so, try to reconnect
               }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /*
    Called when the user minimizes out of the app. Closes the bluetooth socket so transmission stops
     */
    @Override
    public void onPause() {
        super.onPause();
        bluetoothConnection.closeSocket();
    }
    /*
    This TimerTask is executed at a rate of 10hz, it simply starts the process of sending data to the arduino
     */
    public class CommunicationTimerTask extends TimerTask {
        public void run(){
            if(bluetoothConnection.isConnected()){//Checks to see if we are connected to the arduino through bluetooth
              sendData();
            }
        }
    }

    /*
    Unused but required as part of the SensorEventListener interface
     */
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy){}

    /*
    When in Accelerometer mode, will compute the motor speed based on accelerometer inclination
     */
    @Override
    public final void onSensorChanged(SensorEvent event) {


        if(mode == ACCELEROMETER_MODE){
            int x =  (int)Math.floor(event.values[0])+1; //the x-axis is used for left/right movement

            int y =  (int)Math.floor(event.values[1]);//the y-axis is used for forwards/backwards movement
            y = -y;

            //z-axis is available but is not used

            // Map 0 to +10 to 0 to 255 for going forward and back
            int speed = 25*y;
            speedLeft = speed;
            speedRight = speed;

            // If x axis turned more than 6, rotate in that direction
            if (x > 3) {
                speedLeft = -x*25;
                speedRight = x*25;
            }
            else if (x < -2) {
                speedLeft = -x*25;
                speedRight = x*25;
            }
        }
    }

    /*
    * Handler that processes the data sent from the arduino to the app. We use handler so that we can process the data on a different thread
    * than the ConnectionThread in BluetoothConnection.
    * */
    private final Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what) {
                case RECIEVE_MESSAGE: //If we have received a message from arduino                                                   /
                    byte[] readBuf = (byte[]) msg.obj; // reads message as a byte buffer
                    String readMessage= new String(readBuf, 0, msg.arg1); //Convertes byte buffer into string
                    int endIndex = readMessage.indexOf("\n"); //Finds the index of the end of valid string
                    readMessage = readMessage.substring(0, endIndex); //cuts of any invalid characters

                    //Tries to parse a float from the string, then covert that float into an integer for graphing
                    try{
                        float f = Float.parseFloat(readMessage);
                        int distance = Math.round(f);
                        Log.d(TAG, Integer.toString(distance));

                        updateDisplay(distance);//Updates the display graph with the new distance
                    }catch(NumberFormatException e){
                        Log.d(TAG, "Problem reading distance from arduino");
                    }
                    break;
            }
        }
    };


}
