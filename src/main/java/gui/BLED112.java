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
package gui;


import org.thingml.bglib.BGAPITransport;
import purejavacomm.CommPort;
import purejavacomm.CommPortIdentifier;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ffl
 */
public class BLED112 {
    /* ***********************************************************************
     * Serial port utilities: listing
     *************************************************************************/
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

    public static void registerPort(String port) {
        
    	String prop = System.getProperty("gnu.io.rxtx.SerialPorts");
        if (prop == null) {
            prop = "";
        }
        if (!prop.contains(port)) {
            prop += port + File.pathSeparator;
            System.setProperty("gnu.io.rxtx.SerialPorts", prop);
        }
        System.out.println("gnu.io.rxtx.SerialPorts = " + prop);

        prop = System.getProperty("javax.comm.rxtx.SerialPorts");
        if (prop == null) {
            prop = "";
        }
        if (!prop.contains(port)) {
            prop += port + File.pathSeparator;
            System.setProperty("javax.comm.rxtx.SerialPorts", prop);
        }
        System.out.println("javax.comm.rxtx.SerialPorts = " + prop);
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
        
       return (String) JOptionPane.showInputDialog(
               null,
               "BLED112",
               "Select serial port",
               JOptionPane.PLAIN_MESSAGE,
               null,
               possibilities.toArray(),
               possibilities.toArray()[startPosition]);
        
    }
    
    public static BGAPITransport connectBLED112(String portName) {
        SerialPort port = connectSerial(portName);
        try {
            return new BGAPITransport(port.getInputStream(), port.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(BLED112.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static SerialPort connectSerial(String portName) {
    	try {
    		
    		//String portName = selectSerialPort();
    		
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
