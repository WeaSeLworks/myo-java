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


import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.KNearestNeighbors;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.data.FileHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingml.bglib.BDAddr;
import org.thingml.bglib.BGAPI;
import org.thingml.bglib.BGAPIDefaultListener;

import javax.annotation.PreDestroy;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static net.sf.javaml.tools.data.FileHandler.loadDataset;

public class MyoApplication extends BGAPIDefaultListener
{

    public static final String PAULSMYO = "c3:69:40:b1:5d:f6";
    public static final int MANUFACTURER = 0x0B;
    public static final int FIRMWARE = 0x0B;
    public static final int MYO_SENSOR_SETTINGS = 0x19;
    public static final int EMG = 0x28;
    public static final int IMU = 0x1d;

    public static final int IMU_VALUE = 28;
    public static final int EMG_VALUE = 39;

    private static final int IDLE = 0;



    private BGAPI client;
    private Set<BDAddr> devices = new HashSet<BDAddr>();
    private int connection;
    private Consumer<BDAddr> deviceFoundAction;
    private Consumer<Integer> connectAction;
    private Consumer<Integer> disconnectAction;
    private Consumer<String> firmwareAction;
    private Consumer<Integer[]> imuAction;
    private Consumer<List<Integer>> emgAction;
    private Consumer<Pose> poseAction;
    private Classifier knn;
    private Semaphore semaphore = new Semaphore(1);





    static Logger logger = LoggerFactory.getLogger(MyoApplication.class);

    public static void main( String[] args )
    {
        MyoApplication myoApplication = new MyoApplication();
        myoApplication.start();
    }


    public MyoApplication(){
        Dataset allPoses = new DefaultDataset();
        allPoses.addAll(loadDatasetFromPoseFile("gestures/fist.data"));
        allPoses.addAll(loadDatasetFromPoseFile("gestures/spread.data"));
        allPoses.addAll(loadDatasetFromPoseFile("gestures/left.data"));
        allPoses.addAll(loadDatasetFromPoseFile("gestures/right.data"));

        this.knn = new KNearestNeighbors(10);
        knn.buildClassifier(allPoses);
    }

    private Dataset loadDatasetFromPoseFile(String filename){
        Dataset newDataset = null;
        InputStream uri = MyoApplication.class.getResourceAsStream(String.format("/%s", filename));
        logger.info("URI = "+uri.toString());
        newDataset = FileHandler.load(new InputStreamReader(uri), 8, ",");
        return  newDataset;
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

    public void disconnect(int connId, Consumer<Integer> disconnectAction) {
        client.send_connection_disconnect(connId);
        this.disconnectAction = disconnectAction;
    }

    public void getFirmwareVersion(Consumer<String> action ) {
        this.firmwareAction = action;
        client.send_attclient_read_by_handle(connection, 0x17);
    }

    public void writeAttr(int handle, byte[] data) {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {

            client.send_attclient_attribute_write(connection, handle, data);
            semaphore.acquire();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void subscribeMyoData(Consumer<Integer[]> imuAction, Consumer<List<Integer>> emgAction) throws ExecutionException, InterruptedException {
        this.imuAction = imuAction;
        this.emgAction = emgAction;
        writeAttr(EMG, new byte[]{0x01, 0x00});
        writeAttr(IMU, new byte[]{0x01, 0x00});
        sendSettings();
    }

    private void sendSettings() throws ExecutionException, InterruptedException {
        int C = 1000;
        int emg_smooth = 100;
        int imu_hz = 50;
        //this is to get around the max value of a byte in java (2^7 -1)
        byte E8 = 0; E8 ^= 0xE8;
        byte[] sensorSettings2 = new byte[]{0x02, 0x09, 0x02, 0x01, E8, 0x03,  0x64, 0x14, 0x32, 0, 0};

        writeAttr(MYO_SENSOR_SETTINGS, sensorSettings2);
    }

    @Override
    public void receive_connection_disconnect(int connection, int result) {
        logger.info("receive_connection_disconnect !!!");
        disconnectAction.accept(connection);
    }

    @Override
    public void receive_gap_scan_response(int rssi, int packet_type, BDAddr sender, int address_type, int bond, byte[] data) {
        logger.debug(String.format("Found device %s", sender));
        if (deviceFoundAction != null) {
            deviceFoundAction.accept(sender);
        }
    }

    @Override
    public void receive_gap_connect_direct(int result, int connection_handle) {
        logger.info(String.format("<<< Connected >>> [%d] Result: %d", connection_handle, result));
        connection = connection_handle;
        connectAction.accept(connection_handle);
    }

    @Override
    public void receive_attclient_attribute_value(int connection, int atthandle, int type, byte[] data) {
        if (this.connection == connection) {
            switch (atthandle) {
                case FIRMWARE:  firmwareInfoReceived(data);
                    break;
                case IMU_VALUE: imuDataReceived(data);
                    break;
                case EMG_VALUE: emgDataReceived(data);
                    break;
                default:        logger.warn("Data received for unknown attr handle"+atthandle);
            }
        }
    }

    public void onPose(Consumer<Pose> poseFunction) {
        this.poseAction = poseFunction;
    }

    private void emgDataReceived(byte[] emgData) {
        int a = ((emgData[1] & 0xFF) << 8) + (emgData[0] & 0xFF); if (a > (1<<15)) { a = a - (1<<16); }
        int b = ((emgData[3] & 0xFF) << 8) + (emgData[2] & 0xFF); if (b > (1<<15)) { b = b - (1<<16); }
        int c = ((emgData[5] & 0xFF) << 8) + (emgData[4] & 0xFF); if (c > (1<<15)) { c = c - (1<<16); }
        int d = ((emgData[7] & 0xFF) << 8) + (emgData[6] & 0xFF); if (d > (1<<15)) { d = d - (1<<16); }
        int e = ((emgData[9] & 0xFF) << 8) + (emgData[8] & 0xFF); if (e > (1<<15)) { e = e - (1<<16); }
        int f = ((emgData[11] & 0xFF) << 8) + (emgData[10] & 0xFF); if (f > (1<<15)) { f = f - (1<<16); }
        int g = ((emgData[13] & 0xFF) << 8) + (emgData[12] & 0xFF); if (g > (1<<15)) { g = g - (1<<16); }
        int h = ((emgData[15] & 0xFF) << 8) + (emgData[14] & 0xFF); if (h > (1<<15)) { h = h - (1<<16); }
        if (emgAction != null) {
            emgAction.accept(Arrays.asList(new Integer[]{a, b, c, d, e, f, g, h}));
        }
        Instance instance = new DenseInstance(new double[]{a, b, c, d, e, f, g, h});
        String classification = (String) knn.classify(instance);
        Pose pose = Pose.fromString(classification);
        if (pose.isKnownPose()){
            logger.info(String.format("Pose: %s ", classification));
            poseAction.accept(pose);
        }
        logger.debug(String.format("EMG: %d %d %d %d %d %d %d %d ", a, b, c, d, e, f, g, h));
    }

    private void imuDataReceived(byte[] imuData) {

        int gx = ((imuData[1] & 0xFF) << 8) + (imuData[0] & 0xFF); if (gx > (1<<15)) { gx = gx - (1<<16); }
        int gy = ((imuData[3] & 0xFF) << 8) + (imuData[2] & 0xFF); if (gy > (1<<15)) { gy = gy - (1<<16); }
        int gz = ((imuData[5] & 0xFF) << 8) + (imuData[4] & 0xFF); if (gz > (1<<15)) { gz = gz - (1<<16); }

        int ax = ((imuData[7] & 0xFF) << 8) + (imuData[6] & 0xFF); if (ax > (1<<15)) { ax = ax - (1<<16); }
        int ay = ((imuData[9] & 0xFF) << 8) + (imuData[8] & 0xFF); if (ay > (1<<15)) { ay = ay - (1<<16); }
        int az = ((imuData[11] & 0xFF) << 8) + (imuData[10] & 0xFF); if (az > (1<<15)) { az = az - (1<<16); }

        imuAction.accept(new Integer[]{gx, gy, gz, ax, ay, az});

        logger.debug(String.format("IMU gx: %d  gy: %d  gz: %d  ax: %d  ay: %d  az: %d", gx, gy, gz, ax, ay, az));
    }


    private void firmwareInfoReceived(byte[] value) {
        String ver = String.format("%d.%d.%d.%d", (int) value[0], (int) value[1], (int) value[2], (int) value[3]);
        firmwareAction.accept(ver);
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
    public void receive_attclient_attribute_write(int connection, int result) {
        logger.info("receive_attclient_attribute_write !!!");
    }

    @Override
    public void receive_attclient_procedure_completed(int connection, int result, int chrhandle) {
        semaphore.release();
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
