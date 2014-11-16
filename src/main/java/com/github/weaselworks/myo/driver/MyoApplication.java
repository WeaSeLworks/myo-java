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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingml.bglib.BDAddr;
import org.thingml.bglib.BGAPI;
import org.thingml.bglib.BGAPIDefaultListener;

import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

public class MyoApplication extends BGAPIDefaultListener
{

    public static final String PAULSMYO = "c3:69:40:b1:5d:f6";
    public static final int MANUFACTURER = 0x0B;


    private BGAPI client;
    private Set<BDAddr> devices = new HashSet<BDAddr>();
    private int connection;
    private Consumer<BDAddr> deviceFoundAction;
    private Consumer<Integer> connectAction;
    private Consumer<Integer> disconnectAction;



    static Logger logger = LoggerFactory.getLogger(MyoApplication.class);

    public static void main( String[] args )
    {
        MyoApplication myoApplication = new MyoApplication();
        myoApplication.start();
    }


    public MyoApplication(){

    }

    public void start(){
        logger.info( "Connecting BLED112 Dongle..." );

        client = BluetoothClientFactory.instance();
        client.addListener(this);


        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            logger.error("Unable to load the library",ex);
        }
        logger.info("Requesting Version Number...");
        client.send_system_get_info();
        client.send_system_hello();
    }

    public void onDeviceFound(Consumer<BDAddr> action) {
        deviceFoundAction = action;
        client.send_gap_set_scan_parameters(200, 400, 1);
        client.send_gap_discover(1);
        logger.info("Scanning for devices...");
    }

    public void connect(String bluetoothAddress, Consumer<Integer> connectAction){
        client.send_gap_connect_direct(BDAddr.fromString(bluetoothAddress), 0, 6, 6, 100, 0);
        this.connectAction = connectAction;
    }

    public void disconnect(int connId, Consumer<Integer> disconnectAction){
        client.send_connection_disconnect(connId);
        this.disconnectAction = disconnectAction;
    }


    @Override
    public void receive_connection_disconnect(int connection, int result) {
        logger.info("receive_connection_disconnect !!!");
        disconnectAction.accept(connection);
    }

    @Override
    public void receive_gap_scan_response(int rssi, int packet_type, BDAddr sender, int address_type, int bond, byte[] data) {
        logger.info(String.format("Found device %s",sender));
        if (deviceFoundAction != null) {
            deviceFoundAction.accept(sender);
        }
    }


    @Override
    public void receive_gap_connect_direct(int result, int connection_handle) {
        logger.info(String.format("<<< Connected >>> [%d] Result: %d", connection_handle, result));
        connection = connection_handle;

        connectAction.accept(connection_handle);



//        client.send_attclient_read_by_handle(connection_handle, 0x17);
//        while(true) {
//            logger.info(".");
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                //swallow
//            }
//            client.send_connection_get_status(connection_handle);
//        }

        //client.send_attr(connection_handle, 23);
        //client.send_attclient_read_by_handle(connection_handle, 0x17);
        //client.send

    }

    @Override
    public void receive_attclient_read_by_handle(int connection, int result) {
        logger.info(String.format("receive_attclient_read_by_handle !!! %d %d",connection, result));
    }

    @Override
    public void receive_attributes_read(int handle, int offset, int result, byte[] value) {
        logger.info(String.format("handle[%d] offset[%d] result[%d]    ",handle, offset, result)+bytesToHex(value));

    }

    public static String bytesToHex(byte[] bytes ){
        StringBuffer result = new StringBuffer();
        result.append("[");

        for (byte b : bytes) {
            result.append( Integer.toHexString((int) (b & 0xFF)) + " ");
        }

        result.append("]");

        return result.toString();
    }

    @Override
    public void receive_connection_status(int connection, int flags, BDAddr address, int address_type, int conn_interval, int timeout, int latency, int bonding) {
        logger.info(" !!!");
    }



    @Override
    public void receive_connection_features_get(int connection, int result) {
        logger.info("receive_connection_features_get !!! "+ result);
    }

    @Override
    public void receive_system_reset() {
        logger.info("receive_system_reset !!!");
    }

    @Override
    public void receive_system_hello() {
        logger.info("receive_system_hello !!!");
    }

    @Override
    public void receive_system_address_get(BDAddr address) {
        logger.info("receive_system_address_get !!!");
    }

    @Override
    public void receive_system_reg_write(int result) {
        logger.info("receive_system_reg_write !!!");
    }

    @Override
    public void receive_system_reg_read(int address, int value) {
        logger.info("resystem_reg_read !!!");
    }

    @Override
    public void receive_system_get_counters(int txok, int txretry, int rxok, int rxfail) {
        logger.info("receive_system_get_counters !!!");
    }

    @Override
    public void receive_system_get_connections(int maxconn) {
        logger.info("receive_system_get_connections !!!");
    }

    @Override
    public void receive_system_read_memory(int address, byte[] data) {
        logger.info("receive_system_read_memory !!!");
    }

    @Override
    public void receive_system_get_info(int major, int minor, int patch, int build, int ll_version, int protocol_version, int hw) {
        logger.info(String.format("Version: %d.%d.%d-%d Protcol-Version: %d",major,minor,patch,build,protocol_version));
    }

    @Override
    public void receive_system_endpoint_tx() {
        logger.info("receive_system_endpoint_tx !!!");
    }

    @Override
    public void receive_system_whitelist_append(int result) {
        logger.info("receive_system_whitelist_append !!!");
    }

    @Override
    public void receive_system_whitelist_remove(int result) {
        logger.info("receive_system_whitelist_remove !!!");
    }

    @Override
    public void receive_system_whitelist_clear() {
        logger.info("receive_system_whitelist_clear !!!");
    }

    @Override
    public void receive_system_boot(int major, int minor, int patch, int build, int ll_version, int protocol_version, int hw) {
        logger.info("receive_system_boot !!!");
    }

    @Override
    public void receive_system_debug(byte[] data) {
        logger.info("receive_system_debug !!!");
    }

    @Override
    public void receive_system_endpoint_rx(int endpoint, byte[] data) {
        logger.info("receive_system_endpoint_rx !!!");
    }

    @Override
    public void receive_flash_ps_defrag() {
        logger.info("receive_flash_ps_defrag !!!");
    }

    @Override
    public void receive_flash_ps_dump() {
        logger.info("receive_flash_ps_dump !!!");
    }

    @Override
    public void receive_flash_ps_erase_all() {
        logger.info("receive_flash_ps_erase_all !!!");
    }

    @Override
    public void receive_flash_ps_save(int result) {
        logger.info("receive_flash_ps_save !!!");
    }

    @Override
    public void receive_flash_ps_load(int result, byte[] value) {
        logger.info("receive_flash_ps_load !!!");
    }

    @Override
    public void receive_flash_ps_erase() {
        logger.info("receive_flash_ps_erase !!!");
    }

    @Override
    public void receive_flash_erase_page(int result) {
        logger.info("receive_flash_erase_page !!!");
    }

    @Override
    public void receive_flash_write_words() {
        logger.info("receive_flash_write_words !!!");
    }

    @Override
    public void receive_flash_ps_key(int key, byte[] value) {
        logger.info("receive_flash_ps_key !!!");
    }

    @Override
    public void receive_attributes_write(int result) {
        logger.info("receive_attributes_write !!!");
    }



    @Override
    public void receive_attributes_read_type(int handle, int result, byte[] value) {
        logger.info("receive_attributes_read_type !!!");
    }

    @Override
    public void receive_attributes_user_response() {
        logger.info("receive_attributes_user_response !!!");
    }

    @Override
    public void receive_attributes_value(int connection, int reason, int handle, int offset, byte[] value) {
        logger.info("receive_attributes_value !!!");
    }

    @Override
    public void receive_attributes_user_request(int connection, int handle, int offset) {
        logger.info("receive_attributes_user_request !!!");
    }

    @Override
    public void receive_connection_get_rssi(int connection, int rssi) {
        logger.info("receive_connection_get_rssi !!!");
    }

    @Override
    public void receive_connection_update(int connection, int result) {
        logger.info("receive_connection_update !!!");
    }

    @Override
    public void receive_connection_version_update(int connection, int result) {
        logger.info("receive_connection_version_update !!!");
    }

    @Override
    public void receive_connection_channel_map_get(int connection, byte[] map) {
        logger.info("receive_connection_channel_map_get !!!");
    }

    @Override
    public void receive_connection_channel_map_set(int connection, int result) {
        logger.info("receive_connection_channel_map_set !!!");
    }


    @Override
    public void receive_connection_get_status(int connection) {
        logger.info("receive_connection_get_status !!!");
    }

    @Override
    public void receive_connection_raw_tx(int connection) {
        logger.info("receive_connection_raw_tx !!!");
    }

    @Override
    public void receive_connection_version_ind(int connection, int vers_nr, int comp_id, int sub_vers_nr) {
        logger.info("receive_connection_version_ind !!!");
    }

    @Override
    public void receive_connection_feature_ind(int connection, byte[] features) {
        logger.info("receive_connection_feature_ind !!!");
    }

    @Override
    public void receive_connection_raw_rx(int connection, byte[] data) {
        logger.info("receive_connection_raw_rx !!!");
    }

    @Override
    public void receive_connection_disconnected(int connection, int reason) {
        logger.info("receive_connection_disconnected !!!");
    }

    @Override
    public void receive_attclient_find_by_type_value(int connection, int result) {
        logger.info("receive_attclient_find_by_type_value !!!");
    }

    @Override
    public void receive_attclient_read_by_group_type(int connection, int result) {
        logger.info("receive_attclient_read_by_group_type !!!");
    }

    @Override
    public void receive_attclient_read_by_type(int connection, int result) {
        logger.info("receive_attclient_read_by_type !!!");
    }

    @Override
    public void receive_attclient_find_information(int connection, int result) {
        logger.info("receive_attclient_find_information !!!");
    }

    @Override
    public void receive_attclient_attribute_write(int connection, int result) {
        logger.info("receive_attclient_attribute_write !!!");
    }

    @Override
    public void receive_attclient_write_command(int connection, int result) {
        logger.info("receive_attclient_write_command !!!");
    }

    @Override
    public void receive_attclient_reserved() {
        logger.info("receive_attclient_reserved !!!");
    }

    @Override
    public void receive_attclient_read_long(int connection, int result) {
        logger.info("receive_attclient_read_long !!!");
    }

    @Override
    public void receive_attclient_prepare_write(int connection, int result) {
        logger.info("receive_attclient_prepare_write !!!");
    }

    @Override
    public void receive_attclient_execute_write(int connection, int result) {
        logger.info("receive_attclient_execute_write !!!");
    }

    @Override
    public void receive_attclient_read_multiple(int connection, int result) {
        logger.info("receive_attclient_read_multiple !!!");
    }

    @Override
    public void receive_attclient_indicated(int connection, int attrhandle) {
        logger.info("receive_attclient_indicated !!!");
    }

    @Override
    public void receive_attclient_procedure_completed(int connection, int result, int chrhandle) {
        logger.info("receive_attclient_procedure_completed !!!");
    }

    @Override
    public void receive_attclient_group_found(int connection, int start, int end, byte[] uuid) {
        logger.info("receive_attclient_group_found !!!");
    }

    @Override
    public void receive_attclient_attribute_found(int connection, int chrdecl, int value, int properties, byte[] uuid) {
        logger.info("receive_attclient_attribute_found !!!");
    }

    @Override
    public void receive_attclient_find_information_found(int connection, int chrhandle, byte[] uuid) {
        logger.info("receive_attclient_find_information_found !!!");
    }

    @Override
    public void receive_attclient_attribute_value(int connection, int atthandle, int type, byte[] value) {
        logger.info("receive_attclient_attribute_value !!!");
    }

    @Override
    public void receive_attclient_read_multiple_response(int connection, byte[] handles) {
        logger.info("receive_attclient_read_multiple_response !!!");
    }

    @Override
    public void receive_sm_encrypt_start(int handle, int result) {
        logger.info("receive_sm_encrypt_start !!!");
    }

    @Override
    public void receive_sm_set_bondable_mode() {
        logger.info("receive_sm_set_bondable_mode !!!");
    }

    @Override
    public void receive_sm_delete_bonding(int result) {
        logger.info("receive_sm_delete_bonding !!!");
    }

    @Override
    public void receive_sm_set_parameters() {
        logger.info("receive_sm_set_parameters !!!");
    }

    @Override
    public void receive_sm_passkey_entry(int result) {
        logger.info("receive_sm_passkey_entry !!!");
    }

    @Override
    public void receive_sm_get_bonds(int bonds) {
        logger.info("receive_sm_get_bonds !!!");
    }

    @Override
    public void receive_sm_set_oob_data() {
        logger.info("receive_sm_set_oob_data !!!");
    }

    @Override
    public void receive_sm_smp_data(int handle, int packet, byte[] data) {
        logger.info("receive_sm_smp_data !!!");
    }

    @Override
    public void receive_sm_bonding_fail(int handle, int result) {
        logger.info("receive_sm_bonding_fail !!!");
    }

    @Override
    public void receive_sm_passkey_display(int handle, int passkey) {
        logger.info("receive_sm_passkey_display !!!");
    }

    @Override
    public void receive_sm_passkey_request(int handle) {
        logger.info("receive_sm_passkey_request !!!");
    }

    @Override
    public void receive_sm_bond_status(int bond, int keysize, int mitm, int keys) {
        logger.info("receive_sm_bond_status !!!");
    }

    @Override
    public void receive_gap_set_privacy_flags() {
        logger.info("receive_gap_set_privacy_flags !!!");
    }

    @Override
    public void receive_gap_set_mode(int result) {
        logger.info("receive_gap_set_mode !!!");
    }

    @Override
    public void receive_gap_discover(int result) {
        logger.info("receive_gap_discover !!!");
    }

    @Override
    public void receive_gap_end_procedure(int result) {
        logger.info("receive_gap_end_procedure !!!");
    }

    @Override
    public void receive_gap_connect_selective(int result, int connection_handle) {
        logger.info("receive_gap_connect_selective !!!");
    }

    @Override
    public void receive_gap_set_filtering(int result) {
        logger.info("receive_gap_set_filtering !!!");
    }

    @Override
    public void receive_gap_set_scan_parameters(int result) {
        logger.info("receive_gap_set_scan_parameters !!!");
    }

    @Override
    public void receive_gap_set_adv_parameters(int result) {
        logger.info("receive_gap_set_adv_parameters !!!");
    }

    @Override
    public void receive_gap_set_adv_data(int result) {
        logger.info("receive_gap_set_adv_data !!!");
    }

    @Override
    public void receive_gap_set_directed_connectable_mode(int result) {
        logger.info("receive_gap_set_directed_connectable_mode !!!");
    }

    @Override
    public void receive_gap_mode_changed(int discover, int connect) {
        logger.info("receive_gap_mode_changed !!!");
    }

    @Override
    public void receive_hardware_io_port_config_irq(int result) {
        logger.info("receive_hardware_io_port_config_irq !!!");
    }

    @Override
    public void receive_hardware_set_soft_timer(int result) {
        logger.info("receive_hardware_set_soft_timer !!!");
    }

    @Override
    public void receive_hardware_adc_read(int result) {
        logger.info("receive_hardware_adc_read !!!");
    }

    @Override
    public void receive_hardware_io_port_config_direction(int result) {
        logger.info("receive_hardware_io_port_config_direction !!!");
    }

    @Override
    public void receive_hardware_io_port_config_function(int result) {
        logger.info("receive_hardware_io_port_config_function !!!");
    }

    @Override
    public void receive_hardware_io_port_config_pull(int result) {
        logger.info("receive_hardware_io_port_config_pull !!!");
    }

    @Override
    public void receive_hardware_io_port_write(int result) {
        logger.info("receive_hardware_io_port_write !!!");
    }

    @Override
    public void receive_hardware_io_port_read(int result, int port, int data) {
        logger.info("receive_hardware_io_port_read !!!");
    }

    @Override
    public void receive_hardware_spi_config(int result) {
        logger.info("receive_hardware_spi_config !!!");
    }

    @Override
    public void receive_hardware_spi_transfer(int result, int channel, byte[] data) {
        logger.info("receive_hardware_spi_transfer !!!");
    }

    @Override
    public void receive_hardware_i2c_read(int result, byte[] data) {
        logger.info("receive_hardware_i2c_read !!!");
    }

    @Override
    public void receive_hardware_i2c_write(int written) {
        logger.info("receive_hardware_i2c_write !!!");
    }

    @Override
    public void receive_hardware_set_txpower() {
        logger.info("receive_hardware_set_txpower !!!");
    }

    @Override
    public void receive_hardware_io_port_status(int timestamp, int port, int irq, int state) {
        logger.info("receive_hardware_io_port_status !!!");
    }

    @Override
    public void receive_hardware_soft_timer(int handle) {
        logger.info("receive_hardware_soft_timer !!!");
    }

    @Override
    public void receive_hardware_adc_result(int input, int value) {
        logger.info("receive_hardware_adc_result !!!");
    }

    @Override
    public void receive_test_phy_tx() {
        logger.info("receive_test_phy_tx !!!");
    }

    @Override
    public void receive_test_phy_rx() {
        logger.info("receive_test_phy_rx !!!");
    }

    @Override
    public void receive_test_phy_end(int counter) {
        logger.info("receive_test_phy_end !!!");
    }

    @Override
    public void receive_test_phy_reset() {
        logger.info("receive_test_phy_reset !!!");
    }

    @Override
    public void receive_test_get_channel_map(byte[] channel_map) {
        logger.info("receive_test_get_channel_map !!!");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Destorying MyoApplication Instance");
        //client.removeListener(this);
        if (connection >= 0) {
            client.send_connection_disconnect(connection);
        }

        if (client != null) {
            client.removeListener(this);
        }

        BluetoothClientFactory.disconnectBLED112();
    }
}
