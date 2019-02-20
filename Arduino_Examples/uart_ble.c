#include <LBLE.h>
#include <LBLEPeriphral.h>
#include "Arduino.h"

const int sensor = 14;
const int usr_btn = 6;
const int SAMPLE_RATE = 1000;
bool isToggle = false;
String data;

LBLEService uartService("00001802-0000-1000-8000-00805f9b34fb");
LBLECharacteristicString rxCharacteristic("00002a04-0000-1000-8000-00805f9b34fb", LBLE_READ | LBLE_WRITE);//Rx
LBLECharacteristicString txCharacteristic("00002a05-0000-1000-8000-00805f9b34fb", LBLE_READ | LBLE_WRITE);//Rx
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
  char* devName = "LBLE-test";
  Serial.println("BLE ready");
  Serial.print("Device Address = [");
  Serial.print(LBLE.getDeviceAddress());
  Serial.println("]");
  Serial.print("BLE Name = ");
  Serial.println(devName);
  
  LBLEAdvertisementData advertisement;
  advertisement.configAsConnectableDevice(devName);
  LBLEPeripheral.setName(devName);

  uartService.addAttribute(txCharacteristic);
  uartService.addAttribute(rxCharacteristic);

  LBLEPeripheral.addService(uartService);
  LBLEPeripheral.advertise(advertisement);

  LBLEPeripheral.begin();
  
  pinMode(sensor, INPUT);
  attachInterrupt(usr_btn, pin_change, RISING);

}

void loop() { 
  delay(SAMPLE_RATE);
    
  blink_status = !blink_status;

  Serial.print("conected=");
  Serial.println(LBLEPeripheral.connected());
  if(LBLEPeripheral.connected())
  {
    int r = analogRead(sensor);
    Serial.println(r);
    String result = "%D="+r;
    txCharacteristic.setValue(result);
    LBLEPeripheral.notifyAll(txCharacteristic);
  }

  if(rxCharacteristic.isWritten()){
    data = rxCharacteristic.getValue();
    Serial.print("Rx=");
    Serial.println(data);
  }

}


void pin_change(void) 
{ 
  
} 

