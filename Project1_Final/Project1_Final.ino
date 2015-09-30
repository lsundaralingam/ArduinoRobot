#include <LiquidCrystal.h>
#include <SoftwareSerial.h>

#define IDLE_MODE "i"   // Defines the idle mode
#define AUTO_MODE "a"   // Defines the mode for the first functionality
#define MANUAL_MODE "m" // Defines the mode for the second functionality
#define TEMP A5         // Defines the pin used for the LM35 sensor

/* Define the currentStates */
#define FORWARD 0 	// Sets the forward state as 0 
#define LEFT 1 	        // Sets the left state as 1
#define SLOWING 2       // Sets the slowing state as 2
#define STARTSTATE 3	// Sets the start state as 3
#define REVERSE 4       // Sets the reverse state as 4
#define IDLE 5          // Sets the idle state as 5

// Set the current currentState to be forward intially
int currentState;

//Arduino PWM Speed Control：
int E1 = 5; // Motor 1 PWM
int M1 = 4; // Motor 1 Direction control
int E2 = 6; // Motor 2 PWM                      
int M2 = 7; // Motor 2 Direction control   

int TRIG = 2; // Ultrasonic range sensor trigger pin
int ECHO = 3; // Ultrasonic range sensor echo pin

int warningDistance = 30;  // Initialize the warning distance in cm
int criticalDistance = 20; // Initialize the distance at which the robot will stop in cm

String operatingMode; // the string that will define what functionality to operate in

int maxSpeed = 238;     // The value sent to the motors which indicates the max speed
int turningSpeed = 100; // The value sent to the motors which indicates the turning speed
int rightWheelSpeed;    // Initialize the right wheel speed
int leftWheelSpeed;     // Initiialize the left wheel speed

double corFactor = 1.042; // The correction factor

String s; // Initialize string to get from Android app

SoftwareSerial bluetoothSerial(13,12); // Initialize the serial port to be used for the bluetooth module 
char incomingByte;                     // Initializes the serial port used for the bluetooth module

int motor1, motor2; // Initialize the motors
int speed1, speed2; // Initialize the speeds

LiquidCrystal lcd(A4,A3,8,9,10,11); // Initializes the pins used for the LCD

void setup(){   
    /* Sets appropriate pins to input or output mode */ 
    pinMode(M1, OUTPUT);   // Sets the motor 1 pin as output 
    pinMode(M2, OUTPUT);   // Sets the motor 2 pin as output
    pinMode(TRIG, OUTPUT); // Sets the trig pin for the range finder as output
    pinMode(ECHO, INPUT);  // Sets the echo pin for the range finder as output
    
    /* Sets the direction at which the motors will spin */
    digitalWrite(M1, HIGH); // Sets the first motor to spin forwards
    digitalWrite(M2, HIGH); // Sets the second motor to spin forwards
    
    operatingMode = IDLE_MODE; // Set operating mode to the first functionality
    currentState = STARTSTATE; // Puts the state as the starting state
  
    lcd.begin(16,2); // Initializes the LCD screen to have 16 columns and 2 rows
    lcd.clear();     // Clears the lcd
    
    Serial.begin(9600);          // Begins the serial port with a baud rate of 9600
    bluetoothSerial.begin(9600); // Begins the serial connection for the bluetooth module with a baud rate of 9600
 } 

void loop(){ 
  String s; // Initialize string to get from Android app
  
  if(bluetoothSerial.available() > 0){
    /* Makes a string of all the incoming bytes received from the bluetooth module */
    while(bluetoothSerial.available() > 0){
      incomingByte = bluetoothSerial.read(); // Reads the incoming byte from the bluetooth module 
      s += incomingByte; // Concatenates a string with the old string and the current incoming byte
    }
    
    float distance = getDistance();    // Get the distance and set it to distance
    bluetoothSerial.println(distance); // Print the distance to the bluetooth module
  }
  
  operatingMode = s.substring(0,1); // Sets the operating mode depending on the first value sent from the bluetooth module
 
  //* Decides which functionality to operate with */
  if(operatingMode == AUTO_MODE){// Checks if the operating mode is the basic functionality
    basicFunctionality(); // Calls the basic functionality function
  }else if(operatingMode == MANUAL_MODE){ // Checks if the operating mode is the extra functionality
    lcdPrint("Manual Control"); // Print Manual control on LCD
    extraFunctionality(s); // Calls the extra functionality function
  }else if(operatingMode == IDLE_MODE){
    lcdPrint("Idleing"); // Print Idleing on LCD
    idle(); // Calls idle mode
  }
  
  delay(30); // Delay for 30 milliseconds 
}

void idle(){
  analogWrite(E1, 0); // Changes of the speed the first motor spins to zero
  analogWrite(E2, 0); // Changes of the speed the second motor spins to zero
}

/* function for the basic required functionality */
void basicFunctionality(){
  String lcdMessage; // Initialize variable for LCD message
    
  float distance = getDistance(); // Gets the current distance from the range finder 

  /* Checks the distance to the closest object in front of the robot and changes the state accordingly */
  if(distance > warningDistance){ // Checks if the the distance is less than the warning distance
    currentState = FORWARD; // If so changes the current state to forward
  }else if(distance <= warningDistance && distance > criticalDistance){ // // Checks if the distance is less then the warning distance but still bigger than the critical distance
    currentState = SLOWING; // If so changes the current state to slowing
  }else if(distance <= criticalDistance && distance != 0){ // Otherwise checks if the distance is less than or equal to the critical distance and that the distance is not equal to zero
    currentState = LEFT; // If so changes the current state to left
  }
    
  /* State 0 (START state): Initializes a distance (because reading hasn't begun) and sets the state to FORWARD */
  if(currentState == STARTSTATE){ // Checks if the currents state is the starting state
    distance = 50; // Set the distance to 50 
    currentState = FORWARD; // Set the current state to forward
  }
  
  /* State 1 (FORWARD State): The robot moves in the forward direction */
  if(currentState == FORWARD){ // Checks if the current state is forward
     digitalWrite(M1, HIGH); // Changes the direction of the motor to spin forwards
    
     /* Writes the speed tto both motors */
     analogWrite(E1, maxSpeed*corFactor); // Changes the speed of the first motor using correction factor.
     analogWrite(E2, maxSpeed); // Changes the speed of the second motor 
     
     int speed1 = (maxSpeed * (34.36/200)); // Set speed1 using conversion formula
     lcdMessage = String(speed1); // Set lcd message to speed
     lcdMessage += " cm/s"; // Add cm/s to lcd message
   
   /* State 2 (SLOWING state): The robot slows down proportionally to distance */ 
   }else if(currentState == SLOWING){ // Otherwise checks if the current state is slowing
      digitalWrite(M1, HIGH); // Changes the direction of the motors to spin forwards
    
      int slowSpeed = stateSpeed(distance); // Sets the slowing speed from the stateSpeed function
    
      analogWrite(E1, maxSpeed*corFactor); // Changes the speed of the first motor using correction factor.
      analogWrite(E2, slowSpeed); // Changes the speed of the second motor
      
      int speed1 = (maxSpeed * (34.36/200)); // Set speed1 using conversion formula
      lcdMessage = String(speed1); // Set lcd message to speed
      lcdMessage += " cm/s"; // Add cm/s to lcd message
    
    /* State 3 (LEFT State): The robot turns left */
    }else if(currentState == LEFT){ // Otherwise checks if the state is left
       lcdMessage = "Turning Left"; // Sets lcdMessage to "Turning Left"
       lcdPrint(lcdMessage); // Calls lcdPrint function
        
       unsigned long leftTime = 700; // Time it takes to turn 90 degrees 
       //Serial.println(currentState)
       unsigned long startTime = millis(); // Save the time that we started turning
    
       digitalWrite(M1, LOW); // Changes the direction the first motor spins to backwards
       
       /* Stay in this loop until leftTime has elapsed */
       while((millis() - startTime) < leftTime) // Checks if the elaplsed time is less than the time to turn left
       {
            // Turn left
            analogWrite(E1, turningSpeed); // Changes the speed the first motor spins to the turning speed 
            analogWrite(E2, turningSpeed); // Changes the speed the second motor spins to the turning speed
        }
        
        currentState = FORWARD; // Set the current state to forward
        
      }else{
        currentState = IDLE; // Set the current state to idle
      }
      lcdPrint(lcdMessage); // Calls lcdPrint function
}

/* function for the extra functionality */
void extraFunctionality(String s){
  int n = s.indexOf("n"); // Gets index of the seperation between the two motor values
   
  String s1 = s.substring(0,n); // Gets the values for the first motor from the incoming string from the bluetooth module
  String s2 = s.substring(n+1); // Gets the values for the second motor from the incoming string from the bluetooth module
  
  motor1 = s1.toInt(); // Converts the first motor value (string) into a useable integer
  motor2 = s2.toInt(); // Converts the second motor value (string) into a useable integer
  
  /* Sets the direction the motors spin to forward if the motor value received is positive */
  if(motor1 >= 0){ // Checks if the first motor value is greater than or equal to 0
    digitalWrite(M1, HIGH); // If so changes the first motor to spin forwards
  /* Sets the direction the motors spin to backwards if the motor value received is negative */  
  }else if(motor1 < 0){ // Otherwise checks if the first motor value is negative
    motor1 *= -1; // Converts the negative value to positive since the motor only accepts values from 0 to 255
    digitalWrite(M1, LOW); // If so changes the first motor to spin backwards
  }
  /* Sets the direction the motors spin to forward if the motor value received is positive */
  if(motor2 >= 0){ // Checks if the second motor value is greater than or equal to 0
    digitalWrite(M2, HIGH); // If so changes the second motor to spin forwards
  /* Sets the direction the motors spin to backwards if the motor value received is negative */    
  }else if(motor2 < 0){ // Otherwise checks if the second motor value is negative
    motor2 *= -1; // Converts the negative value to positive since the motor only accepts values from 0 to 255
    digitalWrite(M2, LOW); // If so changes the second motor to spin backwards
  }
  
  analogWrite(E1, motor1); // Sets the speed for the first motor to spin at
  analogWrite(E2, motor2); // Sets the speed for the second motor to spin at
  
}

/* function that decides the speed of the robot, while we are changing states */
int stateSpeed(int distance){
  
  int slowingSpeed; //Initialize variable for the slowing speed
 
  /* This is to mimic the robot slowing down and stoping when an object is detected in front */ 
  if(distance <= warningDistance && distance >= criticalDistance+5){ // Checks if the distance is less then the warning distance but still bigger than the critical distance plus five
    slowingSpeed = distance * 7; // Sets the speed to be variably changing at a rate of distance * 7
  }else if(distance <= (criticalDistance + 5)&& distance > criticalDistance){ // Otherwise checks if the distance is less than or equal to the critical distance plus 5 but still greater than the critical distance
    slowingSpeed = 125; // Set slowing speed to 125
  }
  else if(distance <= criticalDistance){ // Otherwise check if the distance is less than or equal to the critical distance
    slowingSpeed= 0; // Set slowing speed to 0
  }
  return slowingSpeed; // Return the slowing speed
}

/* function gets the distance of any object in front of the robot */
float getDistance(){
  /* Sends the 10 uS pulse to the range finder */
  digitalWrite(TRIG, HIGH); // Turns the trig pin on
  delayMicroseconds(10);    // Delay for 10 microseconds
  digitalWrite(TRIG, LOW);  // Turns the trig pin off
  
  int temp = analogRead(TEMP); //Reads the temperature
  float speedOfSound = 331.5 + (0.6 * temp); //Computes the speed of sound 
  unsigned long pulseWidth = pulseIn(ECHO, HIGH); // Gets the pusle width from the range finder
  float distance = pulseWidth / ((1 / speedOfSound) * 20000); //Calculates distance from rangefinder using given formula
  return distance; // returns the distance
}

/* Resets LCD and prints a message on the LCD */
void lcdPrint(String message){
  lcd.begin(16,2); // Initializes the LCD screen to have 16 columns and 2 rows
  lcd.clear(); // Clear LCD
  lcd.print(message); // Prints message
}
