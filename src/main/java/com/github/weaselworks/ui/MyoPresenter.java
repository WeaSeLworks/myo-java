
package com.github.weaselworks.ui;

import com.github.weaselworks.myo.driver.MyoApplication;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;


/**
 * Created by paulwatson on 15/11/2014.
 */
public class MyoPresenter implements Initializable {

    private int connectionId =-1;

    private ObservableList<String> data;

    @FXML
    private Button connectButton;

    @FXML
    private Label connectionStatus;

    @FXML
    private ListView<String> deviceList;

    @Inject
    private MyoApplication myo;

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
        String selectedDevice = deviceList.getSelectionModel()
                .getSelectedItem();
        if (!isConnected()) {
            if (selectedDevice != null) {
                myo.connect(selectedDevice, connId -> {
                    Platform.runLater(() -> {
                        connectButton.setText("Disconnect");
                        connectionId = connId;
                        connectionStatus.setText(String.format("Connected [%s]", connId));
                    });
                });
            }
        }
        else {
            myo.disconnect(connectionId, id -> {
                Platform.runLater(() -> {
                    connectButton.setText("Connect");
                    connectionId = -1;
                    connectionStatus.setText("Status: Idle");
                });
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
        ObservableList<String> devices = deviceList.getItems();

        myo.onDeviceFound(addr -> {
            Platform.runLater(() -> {
                if (!devices.contains(addr.toString())) {
                    devices.add(addr.toString());
                }
            });
        });
    }

    private boolean isConnected() {
        return connectionId >= 0;
    }
}
