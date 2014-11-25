
package com.github.weaselworks.ui;

import com.github.weaselworks.myo.driver.MyoApplication;
import com.github.weaselworks.myo.driver.Pose;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.lang.Math.abs;


/**
 * Created by paulwatson on 15/11/2014.
 */
public class MyoPresenter implements Initializable {

    static Logger logger = LoggerFactory.getLogger(MyoPresenter.class);

    private final Timer t = new Timer();

    private int connectionId =-1;

    private boolean neverConnected = true;


    final Map<Pose, Image> images = new HashMap<Pose, Image>();



    @FXML
    private Button connectButton;

    @FXML
    private Label connectionStatus;

    @FXML
    private ListView<BluetoothDevice> deviceList;

    @FXML
    private Button firmwareButton;

    @FXML
    private Label firmware;

    @FXML
    private Label imuData;

    @FXML
    private Label emgData;

    @FXML
    private Label pose;

    @FXML
    private ImageView gesturePic;

    @Inject
    private MyoApplication myo;


    private void subscribeToMyoData()  {
        logger.info("Subscribing to myo data");

        try {
            myo.subscribeMyoData(imus -> {
                Platform.runLater(() -> {
                    imuData.setText(String.format("IMU x: %d y: %d z: %d", divide(imus[0],100) , divide(imus[1],100), divide(imus[2],100)));
                });
            }, emgs -> {
                Platform.runLater(() -> {
                    String emgInfo = emgs.stream().map(Object::toString)
                            .collect(Collectors.joining(", "));
                    //emgData.setText(String.format("EMG %s", emgInfo));
                });
            });
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onClick(MouseEvent mouseEvent) {
        if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
            if(mouseEvent.getClickCount() == 2 && !isConnected()){
                connectToSelected();
            }
        }
    }

    @FXML
    private void connectBtnAction(ActionEvent mouseEvent) {
        connectToSelected();
    }

    private void connectToSelected() {
        BluetoothDevice selectedDevice = deviceList.getSelectionModel()
                .getSelectedItem();
        if (!isConnected()) {
            if (selectedDevice != null) {
                myo.connect(selectedDevice.getAddress().toString(), connId -> {
                    Platform.runLater(() -> {
                        connectButton.setText("Disconnect");
                        connectionId = connId;
                        connectionStatus.setText(String.format("Connected [%s]", connId));

                        schedule(() -> { subscribeToMyoData(); },1500);
                    });
                    imuData.setVisible(true);
                    setLogoVisible(true);
                });
            }
        }
        else {
            myo.disconnect(connectionId, id -> {
                Platform.runLater(() -> {
                    connectButton.setText("Connect");
                    connectionId = -1;
                    connectionStatus.setText("Status: Idle");
                    deviceList.getSelectionModel().clearSelection();
                    imuData.setVisible(false);
                });
                setLogoVisible(false);
            });
        }

    }


    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  <tt>null</tt> if the location is not known.
     * @param resources The resources used to localize the root object, or <tt>null</tt> if
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        myo.start();
        ObservableList<BluetoothDevice> devices = deviceList.getItems();
        images.put(Pose.FIST, new Image("com/github/weaselworks/ui/Fist.png"));
        images.put(Pose.SPREAD, new Image("com/github/weaselworks/ui/Spread.png"));
        images.put(Pose.LEFT, new Image("com/github/weaselworks/ui/Left.png"));
        images.put(Pose.RIGHT, new Image("com/github/weaselworks/ui/Right.png"));


        deviceList.setCellFactory(cellData -> {
            ListCell<BluetoothDevice> cell = new ListCell<BluetoothDevice>() {
                @Override
                protected void updateItem(BluetoothDevice device, boolean bln) {
                    super.updateItem(device, bln);
                    if (device != null) {
                        setText(device.getAlias());
                    }
                }
            };
            return cell;

        });

        myo.onDeviceFound(addr -> {
            Platform.runLater(() -> {
                BluetoothDevice newDevice = new BluetoothDevice(addr);
                if (!deviceList.getItems().contains(newDevice)) {
                    devices.add(newDevice);
                    if (neverConnected && connectionId < 0) {
                        deviceList.getSelectionModel().select(newDevice);
                        connectToSelected();
                        imuData.setVisible(true);
                        neverConnected = false;
                    }
                }
            });
        });

        myo.onPose(pose -> {
            Platform.runLater(() -> {
                this.pose.setText(pose.getName());
                this.gesturePic.setImage(images.get(pose));
            });

            //only display for a short period of time
            final Label theLabel = this.pose;
            schedule(() -> {
                Platform.runLater(() -> {
                    theLabel.setText("");
                    gesturePic.setImage(null);
                });
            },500);

        });
    }


    private void setLogoVisible(boolean visible){
        String styleClass = visible ? "bg-connected" : "bg";
        Platform.runLater(() -> {
            gesturePic.getParent().getParent().getStyleClass().clear();
            gesturePic.getParent().getParent().getStyleClass().add(styleClass);
        });
    }


    private  static long divide(long num, long divisor) {
        int sign = (num > 0 ? 1 : -1) * (divisor > 0 ? 1 : -1);
        return sign * (abs(num) + abs(divisor) - 1) / abs(divisor);
    }



    public TimerTask schedule(final Runnable r, long delay) {
        final TimerTask task = new TimerTask() { public void run() { r.run(); }};
        t.schedule(task, delay);
        return task;
    }

    /**
     * called on exit
     */
    public void shutdown() {
        t.cancel();
        t.purge();
    }

    private boolean isConnected() {
        return connectionId >= 0;
    }
}
