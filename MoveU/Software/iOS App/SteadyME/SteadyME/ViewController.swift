//
//  ViewController.swift
//  SteadyME
//
//  Created by Abhinandan Jain on 4/8/19.
//  Copyright © 2019 Abhinandan Jain. All rights reserved.
//

import UIKit
import Foundation
import CoreBluetooth

class ViewController: UIViewController,CBCentralManagerDelegate,CBPeripheralDelegate {

    @IBOutlet weak var sendButton: UIButton!
    @IBOutlet weak var resetButton: UIButton!
    @IBOutlet weak var highSlider: UISlider!
    @IBOutlet weak var lowSlider: UISlider!
    @IBOutlet weak var strengthSlider: UISlider!
    @IBOutlet weak var statusText: UILabel!
    @IBOutlet weak var highSliderText: UILabel!
    @IBOutlet weak var lowSliderText: UILabel!
    @IBOutlet weak var strengthSliderText: UILabel!
    @IBOutlet weak var frequencyText: UILabel!
    @IBOutlet weak var sensorText: UILabel!
    @IBOutlet weak var currentText: UILabel!
    
    
    
    var manager:CBCentralManager!
    var _peripheral:CBPeripheral!
    var sendCharacteristic: CBCharacteristic!
    var loadedService: Bool = true
    
    let NAME = "GVS"
    let UUID_SERVICE = CBUUID(string: "6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    let UUID_WRITE = CBUUID(string: "6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    let UUID_READ = CBUUID(string: "6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    
    override func viewDidLoad() {
        super.viewDidLoad()
        init_device()
        setLayout()
        setParams()
        // Do any additional setup after loading the view, typically from a nib.
    }

    func init_device() {
        manager = CBCentralManager(delegate: self, queue: nil)
        print ("init device")
    }
    
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == CBManagerState.poweredOn {
            print("Buscando a Marc")
            central.scanForPeripherals(withServices: nil, options: nil)
        }
    }
    
    
    // Found a peripheral
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        let device = (advertisementData as NSDictionary).object(forKey: CBAdvertisementDataLocalNameKey) as? NSString
        // Check if this is the device we want
        debugPrint(device)
        if device?.contains(NAME) == true {
            // Stop looking for devices
            // Track as connected peripheral
            // Setup delegate for events
            self.manager.stopScan()
            self._peripheral = peripheral
            self._peripheral.delegate = self
            
            // Connect to the perhipheral proper
            manager.connect(peripheral, options: nil)
            debugPrint("Found Bean.")
        }
    }
    
    // Connected to peripheral
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        // Ask for services
        peripheral.discoverServices(nil)
        debugPrint("Getting services ...")
    }
    
    // Discovered peripheral services
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        // Look through the service list
        for service in peripheral.services! {
            let thisService = service as CBService
            // If this is the service we want
            if service.uuid == UUID_SERVICE {
                // Ask for specific characteristics
                peripheral.discoverCharacteristics(nil, for: thisService)
            }
            debugPrint("Service: ", service.uuid)
        }
    }
    
    // Discovered peripheral characteristics
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        debugPrint("Enabling ...")
        // Look at provided characteristics
        for characteristic in service.characteristics! {
            let thisCharacteristic = characteristic as CBCharacteristic
            // If this is the characteristic we want
            print(thisCharacteristic.uuid)
            if thisCharacteristic.uuid == UUID_READ {
                // Start listening for updates
                // Potentially show interface
                self._peripheral.setNotifyValue(true, for: thisCharacteristic)
                
                // Debug
                debugPrint("Set to notify: ", thisCharacteristic.uuid)
            } else if thisCharacteristic.uuid == UUID_WRITE {
                sendCharacteristic = thisCharacteristic
                loadedService = true
            }
            debugPrint("Characteristic: ", thisCharacteristic.uuid)
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        //print("Data")
        // Make sure it is the peripheral we want
        //    print(characteristic.uuid)
        if characteristic.uuid == UUID_READ {
            // Get bytes into string
            let dataReceived = characteristic.value! as NSData
            //print(dataReceived)
            
            var uindex: UInt16 = 0
            var uAccX: Int16 = 0
            var uAccY: Int16 = 0
            var uAccZ: Int16 = 0
            var uCurr1: Int16 = 0
            var uCurr2: Int16 = 0
            var uCurr3: Int16 = 0
            var uCurr4: Int16 = 0

            dataReceived.getBytes(&uindex, range: NSRange(location: 0, length: 2))
            dataReceived.getBytes(&uCurr1, range: NSRange(location: 2, length: 2))
            dataReceived.getBytes(&uCurr2, range: NSRange(location: 4, length: 2))
            dataReceived.getBytes(&uCurr3, range: NSRange(location: 6, length: 2))
            dataReceived.getBytes(&uCurr4, range: NSRange(location: 8, length: 2))
            dataReceived.getBytes(&uAccX, range: NSRange(location: 10, length: 2))
            dataReceived.getBytes(&uAccY, range: NSRange(location: 12, length: 2))
            dataReceived.getBytes(&uAccZ, range: NSRange(location: 14, length: 2))
            statusText.text = "Connected"
            sensorText.text = "IMU Data : "+String(uAccX)+","+String(uAccY)+","+String(uAccZ)
            currentText.text = "Current Data : "+String(uCurr1)+","+String(uCurr2)+","+String(uCurr3)+","+String(uCurr4)

        }
    }

    var electrode_1 = 0
    var electrode_2 = 0
    var electrode_3 = 0
    
    func getData() -> NSData{
        let state: UInt8 = 20
        let var1:UInt8 = UInt8(electrode_1)
        let var2:UInt8 = UInt8(electrode_2)
        let var3:UInt8 = UInt8(electrode_3)
        var theData : [UInt8] = [ state, var1 , var2 , var3 ]
        print(theData)
        let data = NSData(bytes: &theData, length: theData.count)
        return data
    }
    
    func updateSettings() {
        if loadedService {
            if _peripheral?.state == CBPeripheralState.connected {
                if let characteristic:CBCharacteristic = sendCharacteristic{
                    let data: Data = getData() as Data
                    _peripheral?.writeValue(data,
                                            for: characteristic,
                                            type: CBCharacteristicWriteType.withResponse)
                }
            }
        }
    }
    
    func setLayout(){
        highSliderText.textAlignment = .center
        lowSliderText.textAlignment = .center
        strengthSliderText.textAlignment = .center
        sensorText.textAlignment = .center
        currentText.textAlignment = .center
        statusText.textAlignment = .center
    }
    
    func setParams(){
        highSlider.minimumValue = 0
        highSlider.maximumValue = 250
        highSlider.value = 0
        lowSlider.minimumValue = 0
        lowSlider.maximumValue = 250
        lowSlider.value = 0
        strengthSlider.minimumValue = 0
        strengthSlider.maximumValue = 250
        strengthSlider.value = 0
        electrode_1 = 0
        electrode_2 = 0
        electrode_3 = 0
        statusText.text = "Not Connected"
        highSliderText.text = "Electrode 1 : " + String(electrode_1)
        lowSliderText.text = "Electrode 2 : " + String(electrode_2)
        strengthSliderText.text = "Electrode 3 : " + String(electrode_3)
        frequencyText.text = "Frequency : " + String("0") + " Hz"
        sensorText.text = "IMU Data"
        currentText.text = "Current Data"
    }
    
    @IBAction func sendButtonAction(_ sender: Any) {
        updateSettings()
        if _peripheral?.state != CBPeripheralState.connected{
            statusText.text = "Not Connected"
        }
    }
    
    @IBAction func resetButtonAction(_ sender: Any) {
        setParams()
        updateSettings()
        if _peripheral?.state != CBPeripheralState.connected{
            statusText.text = "Not Connected"
        }
    }
    
    @IBAction func highSliderAction(_ sender: Any) {
        electrode_1 = Int(round(highSlider.value))
        highSliderText.text = "Electrode 1 : " + String(electrode_1)
        //frequencyText.text = "Frequency : " + String((1000/Float(2*(high_time+low_time)))) + " Hz"
    }
    
    @IBAction func lowSliderAction(_ sender: Any) {
        electrode_2 = Int(round(lowSlider.value))
        lowSliderText.text = "Electrode 2 : " + String(electrode_2)
        //frequencyText.text = "Frequency : " + String((1000/Float(2*(high_time+low_time)))) + " Hz"
    }
    
    @IBAction func strengthSliderAction(_ sender: Any) {
        electrode_3 = Int(round(strengthSlider.value))
        strengthSliderText.text = "Electrode 3 : " + String(electrode_3)
    }
    
}

