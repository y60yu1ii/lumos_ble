#include <LBLE.h>
#include <LBLEPeriphral.h>
#include "Arduino.h"

const int sensor = 14;
const int usr_btn = 6;
const int SAMPLE_RATE = 1000;
bool isToggle = false;

LBLEService ambientService("00001802-0000-1000-8000-00805f9b34fb");
LBLECharacteristicInt ambientCharacteristic("00002a05-0000-1000-8000-00805f9b34fb", LBLE_READ | LBLE_WRITE);
int blink_status = 0;

void setup() {  

  // Initialize LED pin
  pinMode(LED_BUILTIN, OUTPUT);
  blink_status = 0;
  digitalWrite(LED_BUILTIN, blink_status);

  //Initialize serial and wait for port to open:
  Serial.begin(9600);

  // Initialize BLE subsystem
  LBLE.begin();
  while (!LBLE.ready()) {
    delay(100);
  }

  String address = LBLE.getDeviceAddress().toString();
//  const int len = address.strlen();
//  String ADDR = address.substring(len - 4 , len);
  char* devName = "AMB-888C";
  Serial.println("BLE ready");
  Serial.print("Device Address = [");
  Serial.print(LBLE.getDeviceAddress());
  Serial.println("]");
  Serial.print("BLE Name = ");
  Serial.println(devName);
    
  // configure our advertisement data.
  // In this case, we simply create an advertisement that represents an
  // connectable device with a device name
  LBLEAdvertisementData advertisement;
  advertisement.configAsConnectableDevice(devName);
  
  // Configure our device's Generic Access Profile's device name
  // Ususally this is the same as the name in the advertisement data.
  LBLEPeripheral.setName(devName);
  // Add characteristics into ambientCharacteristic
  ambientService.addAttribute(ambientCharacteristic);

   // Add service to GATT server (peripheral)
  LBLEPeripheral.addService(ambientService);

  // start advertisment
  LBLEPeripheral.advertise(advertisement);

  // start the GATT server - it is now 
  // available to connect
  LBLEPeripheral.begin();
  
  // put your setup code here, to run once:
  pinMode(sensor, INPUT);
  attachInterrupt(usr_btn, pin_change, RISING);

}

void loop() { 
  delay(SAMPLE_RATE);
    
  blink_status = !blink_status;

//  Serial.print("conected=");
//  Serial.println(LBLEPeripheral.connected());
  if(LBLEPeripheral.connected())
  {
    int r = analogRead(sensor);
    Serial.println(r);
    ambientCharacteristic.setValue(r);
    LBLEPeripheral.notifyAll(ambientCharacteristic);
  }

}


void pin_change(void) 
{ 
  if(LBLEPeripheral.connected())
  {
    ambientCharacteristic.setValue(0);
    LBLEPeripheral.notifyAll(ambientCharacteristic);
  }
} 
