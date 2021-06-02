

#include <Wire.h>
#include <MPU6050.h>
#include <bluefruit.h>

// BLE Service
BLEDis  bledis;
BLEUart bleuart;
BLEBas  blebas;

SoftwareTimer blinkTimer;

//Variables for MPU
MPU6050 mpu;
Vector rawAccel, rawGyro ;

//Analog Pin Configuration
int hr = A2 , eda = A3 ; // Hr is A2 from A7 for now
int temp[2] = {A1,A7};
int curr[3] = {A4,A0,A5}; //[L,C,R]

//Digital Pin Configuration
int led = 7 ; // 7
int gvs[3] = {11,13,12}; // [L,C,R] // Range is 32 - 64 for control of current source
int shdn_supply = 14 ;
int pelt_ph[2] = {22,25} ; // Check pin 22
int pelt_en[2] = {24,17} ; // Check pin 24
int adr = 30, sck = 15, lrck = 9 , md = 23 , mc = 19 , sdin = 16 , scki = 10 ; // Check pin 23 

const float factor = 3.223 ;

int idx = 0 ;
int r = 0 ;
uint8_t rx[3];
uint8_t stgth = 255; // Strength of stim

long t1 = 0 ;

int calib = 0 ;
int val1 = 0 , val2 = 0 ;

void setup() {
    blinkLED(4);
    //pinConfiguration();
    Serial.begin(115200);
    Serial.println("GVS v1.1");
    Serial.println("---------------------------\n");
    blinkLED(4);
    bleSetup();
    blinkLED(4);
    blinkTimer.begin(100, blink_timer_callback);
    initMPU();
    blinkTimer.start();
    randomSeed(analogRead(0));
    t1 = millis();
    
}

void loop()
{
  

  // Forward from BLEUART to HW Serial
  while ( bleuart.available() )
  {
    
    rx[r++] = (uint8_t) bleuart.read();
    if(r==4) {
      r = 0 ;
      activate();
    }
  }

  
  waitForEvent();
}

void pinConfiguration(){
    pinMode(led,OUTPUT);
    pinMode(shdn_supply,OUTPUT);
    pinMode(adr,OUTPUT);
    pinMode(sck,OUTPUT);
    pinMode(lrck,OUTPUT);
    pinMode(md,OUTPUT);
    pinMode(mc,OUTPUT);
    pinMode(sdin,OUTPUT);
    pinMode(scki,OUTPUT);
    for(int i = 0 ; i < 3 ; i++ ){ 
      pinMode(gvs[i],OUTPUT);
      digitalWrite(gvs[i],LOW);
    }
    for(int i = 0 ; i < 2 ; i++ ){
        pinMode(pelt_ph[i],OUTPUT);
        pinMode(pelt_en[i],OUTPUT);
        digitalWrite(pelt_en[i],LOW);
        digitalWrite(pelt_ph[i],LOW);
    }   
    digitalWrite(shdn_supply,HIGH);
    
}

void blinkLED(int n){
    for(int i = 0 ; i < n ; i++){
        digitalWrite(led,HIGH);
        delay(250);
        digitalWrite(led,LOW);
        delay(250);
    }
}

void initMPU(){
    mpu.begin(MPU6050_SCALE_2000DPS, MPU6050_RANGE_2G);
    //mpu.setDHPFMode(MPU6050_DHPF_5HZ);
    mpu.setDHPFMode(MPU6050_DHPF_1_25HZ);
    mpu.calibrateGyro();
}

void readMPU(){
    rawAccel = mpu.readRawAccel();
    rawGyro = mpu.readRawGyro();
//    Serial.print(" Xraw = ");
//    Serial.print(rawAccel.XAxis);
//    Serial.print(" Yraw = ");
//    Serial.print(rawAccel.YAxis);
//    Serial.print(" Zraw = ");
//    Serial.println(rawAccel.ZAxis);   
}  

void bleSetup(){
    Bluefruit.configPrphBandwidth(BANDWIDTH_MAX);
    Bluefruit.begin();
    Bluefruit.setTxPower(4);
    Bluefruit.setName("GVS");
    Bluefruit.setConnectCallback(connect_callback);
    Bluefruit.setDisconnectCallback(disconnect_callback);
    bledis.setManufacturer("MIT Media Lab");
    bledis.setModel("V2.0");
    bledis.begin();
  // Configure and Start BLE Uart Service
    bleuart.begin();
    Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE);
    Bluefruit.Advertising.addTxPower();
    Bluefruit.Advertising.addService(bleuart);
    Bluefruit.ScanResponse.addName();
    Bluefruit.Advertising.restartOnDisconnect(true);
    Bluefruit.Advertising.setInterval(32, 244);   
    Bluefruit.Advertising.setFastTimeout(30);     
    Bluefruit.Advertising.start(0); 
}

void connect_callback(uint16_t conn_handle)
{
  char central_name[32] = { 0 };
  Bluefruit.Gap.getPeerName(conn_handle, central_name, sizeof(central_name));

  Serial.print("Connected to ");
  Serial.println(central_name);
  
}

void disconnect_callback(uint16_t conn_handle, uint8_t reason)
{

  (void) conn_handle;
  (void) reason;
  for(int i = 0 ; i < 3 ; i++ ){ 
      digitalWrite(gvs[i],LOW);
    }
  Serial.println();
  Serial.println("Disconnected");
  
}

void blink_timer_callback(TimerHandle_t xTimerID)
{
  (void) xTimerID;
  digitalToggle(led);
  sendData();
}

void sendData(){
     readMPU();
     int numVals = 8;
     int16_t vals[numVals];
     idx += 1 ;
     vals[0] = idx;
     vals[1] = analogRead(curr[0]);
     vals[2] = analogRead(curr[2]);
     vals[3] = analogRead(curr[1]);
     vals[4] = analogRead(hr);
     vals[5] = int16_t(rawAccel.XAxis) ;
     vals[6] = int16_t(rawAccel.YAxis) ;
     vals[7] = int16_t(rawAccel.ZAxis) ;
     int cnt = numVals * 2 ;
     uint8_t buf[cnt];
     for (int _i=0; _i<numVals; _i++)
        memcpy(&buf[_i*sizeof(int16_t)], &vals[_i], sizeof(int16_t));
     bleuart.write( buf, cnt );
    
}

void rtos_idle_callback(void){
  // Don't call any other FreeRTOS blocking API()
  // Perform background task(s) here
}

void activate(){
  if(rx[0] == 20){
    analogWrite(gvs[0],rx[1]);
    analogWrite(gvs[1],rx[2]);
    analogWrite(gvs[2],rx[3]);
  }  
}

