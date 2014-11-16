package com.github.weaselworks.myo.driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingml.bglib.BGAPI;
import org.thingml.bglib.BGAPITransport;
import purejavacomm.CommPort;
import purejavacomm.CommPortIdentifier;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

/**
 * Created by paulwatson on 11/11/2014.
 */
public class BluetoothClientFactory {

    private final static Logger logger = LoggerFactory.getLogger(BluetoothClientFactory.class);

    private static BGAPI client = null;
    private static SerialPort port = null;

    public static BGAPI instance() {
        if (client == null) {
            BGAPITransport bgapi = connectBLED112();
            client = new BGAPI(bgapi);
        }
        return client;
    }

    public static void disconnectBLED112() {
        //client.getLowLevelDriver().removeListener(logger);
        logger.info("BLE: Reset BLED112 Dongle");
        client.send_system_reset(0);
        client.disconnect();

        if (port != null) {
            port.close();
        }
        client = null;
        port = null;
    }

    private static BGAPITransport connectBLED112() {
        port = connectSerial();
        try {
            return new BGAPITransport(port.getInputStream(), port.getOutputStream());
        } catch (IOException ex) {
            logger.error("Unable to execute connectSerial", ex);
        }
        return null;
    }

    private static SerialPort connectSerial() {
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

    private static String selectSerialPort() {

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




}
