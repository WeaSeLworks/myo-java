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


import de.root1.rxtxrebundled.LibLoader;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingml.bglib.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

public class MyoApplication extends BGAPIDefaultListener
{

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

        LibLoader.loadLibrary("rxtxSerial");

        BGAPITransport bgapi = connectBLED112();
        bgapi.addListener(new BGAPIPacketLogger());
        BGAPI impl = new BGAPI(bgapi);
        impl.addListener(this);

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            logger.error("Unable to load the library",ex);        }
        logger.info("Requesting Version Number...");
        impl.send_system_get_info();
        impl.send_system_hello();
        impl.send_gap_set_scan_parameters(10, 250, 1);
        impl.send_gap_discover(1);

    }

    public void receive_system_get_info(Integer major, Integer minor, Integer patch, Integer build, Integer ll_version, Integer protocol_version, Integer hw) {
        logger.info("get_info_rsp :" + major + "." + minor + "." + patch + " (" + build + ") " + "ll=" + ll_version + " hw=" + hw);
    }

    public void receive_gap_scan_response(Integer rssi, Integer packet_type, BDAddr sender, Integer address_type, Integer bond, byte[] data) {
        logger.info("FOUND: " + sender.toString() + "[" + new String(data).trim() + "] (rssi = " + rssi + ", packet type= " + packet_type + ")");
    }

    @Override
    public void receive_system_hello() {
        logger.info("GOT HELLO!");
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


}