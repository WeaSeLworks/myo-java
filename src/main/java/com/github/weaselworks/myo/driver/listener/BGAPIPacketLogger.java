package com.github.weaselworks.myo.driver.listener;

import org.slf4j.LoggerFactory;
import org.thingml.bglib.BGAPIPacket;
import org.thingml.bglib.BGAPITransportListener;

import org.apache.commons.codec.binary.Hex;


public class BGAPIPacketLogger implements BGAPITransportListener {


    org.slf4j.Logger logger = LoggerFactory.getLogger(BGAPIPacketLogger.class);

    public void packetSent(BGAPIPacket packet) {


            try {

                commonLogger(packet,"sent");
            }
            catch(Exception e)
            {
                logger.error("Unable to receive packet ",e);
            }

        }

    public void packetReceived(BGAPIPacket packet) {

        try {

           commonLogger(packet, "received");
        }
    catch(Exception e)
    {
        logger.error("Unable to receive packet ",e);
    }

    }

    public void commonLogger(BGAPIPacket packet,String prepend)
    {
        try{
            logger.info("packet "+prepend+packet.toString());
        }
        catch(Exception e)
        {
            logger.error("Unable to convert bytes to string ",e);
        }

    }

}