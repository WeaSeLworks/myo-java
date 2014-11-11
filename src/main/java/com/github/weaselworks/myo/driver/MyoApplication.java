/**
 * Copyright (C) 2012 SINTEF <franck.fleurey@sintef.no>
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.weaselworks.myo.driver;


import com.github.weaselworks.myo.driver.listener.BluetoothListener;
import com.github.weaselworks.myo.driver.listener.EmgListener;
import com.github.weaselworks.myo.driver.listener.ImuListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingml.bglib.*;
import purejavacomm.CommPort;
import purejavacomm.CommPortIdentifier;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

public class MyoApplication extends BGAPIDefaultListener
{

    public static final String PAULSMYO = "c3:69:40:b1:5d:f6";


    Logger logger = LoggerFactory.getLogger(MyoApplication.class);

    public static void main( String[] args )
    {
        MyoApplication myoApplication = new MyoApplication();
        myoApplication.loadLibrary();
    }


    public MyoApplication(){

    }

    public void loadLibrary(){

        logger.info( "Connecting BLED112 Dongle..." );

        BGAPITransport bgapi = connectBLED112();
        //bgapi.addListener(new BGAPIPacketLogger());
        BGAPI impl = new BGAPI(bgapi);
        BluetoothListener listener = new BluetoothListener(impl);
        impl.addListener(listener);
        impl.addListener(new EmgListener());
        impl.addListener(new ImuListener());



        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            logger.error("Unable to load the library",ex);        }
        logger.info("Requesting Version Number...");
        impl.send_system_get_info();
        impl.send_system_hello();
        impl.send_gap_set_scan_parameters(10, 250, 1);
        impl.send_gap_discover(1);

        logger.info("Waiting for Myo...");
        while(!listener.getDevices().stream().anyMatch(device -> device.toString().equals(PAULSMYO))) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                //swallow
            }
        }

        try {
            logger.info(String.format("Found %d devices",listener.getDevices().size()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        impl.send_gap_connect_direct(BDAddr.fromString(PAULSMYO), 1, 100, 2000, 15000, 3);
        //impl.send_connection_get_status();


    }


    public  BGAPITransport connectBLED112() {
        SerialPort port = connectSerial();
        try {
            return new BGAPITransport(port.getInputStream(), port.getOutputStream());
        } catch (IOException ex) {
            logger.error("Unable to execute connectSerial", ex);
        }
        return null;
    }

    /**
     * @return    A HashSet containing the CommPortIdentifier for all serial ports that are not currently being used.
     */
    public static HashSet<CommPortIdentifier> getAvailableSerialPorts() {
        HashSet<CommPortIdentifier> h = new HashSet<CommPortIdentifier>();
        Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
        while (thePorts.hasMoreElements()) {
            CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
            switch (com.getPortType()) {
                case CommPortIdentifier.PORT_SERIAL:
                    try {
                        CommPort thePort = com.open("CommUtil", 50);
                        thePort.close();
                        h.add(com);
                    } catch (PortInUseException e) {
                        System.out.println("Port, " + com.getName() + ", is in use.");
                    } catch (Exception e) {
                        System.err.println("Failed to open port " + com.getName());
                        e.printStackTrace();
                    }
            }
        }
        return h;
    }

    public static String selectSerialPort() {

        ArrayList<String> possibilities = new ArrayList<String>();
        //possibilities.add("Emulator");
        for (CommPortIdentifier commportidentifier : getAvailableSerialPorts()) {
            possibilities.add(commportidentifier.getName());
        }

        int startPosition = 0;
        if (possibilities.size() > 1) {
            startPosition = 1;
        }
        //temporary hardcoding
        return "/dev/tty.usbmodem1";

        /*return (String) JOptionPane.showInputDialog(
                null,
                "BLED112",
                "Select serial port",
                JOptionPane.PLAIN_MESSAGE,
                null,
                possibilities.toArray(),
                possibilities.toArray()[startPosition]);*/

    }

    public static SerialPort connectSerial() {
        try {

            String portName = selectSerialPort();

            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

            if (portIdentifier.isCurrentlyOwned()) {
                System.err.println("Error: Port " + portName + " is currently in use");
            }
            else {
                CommPort commPort = portIdentifier.open("BLED112", 2000);

                System.out.println("port = " + commPort);

                if (commPort instanceof SerialPort) {
                    SerialPort serialPort = (SerialPort) commPort;
                    serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

                    serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
                    serialPort.setRTS(true);


                    System.out.println("serial port = " + serialPort);

                    return serialPort;

                } else {
                    System.err.println("Error: Port " + portName + " is not a valid serial port.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String convertHexToString(String hex){

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for( int i=0; i<hex.length()-1; i+=2 ){

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char)decimal);

            temp.append(decimal);
        }
        logger.info("Decimal : " + temp.toString());

        return sb.toString();
    }


}
