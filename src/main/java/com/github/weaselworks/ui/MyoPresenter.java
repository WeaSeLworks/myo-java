
package com.github.weaselworks.ui;

import com.github.weaselworks.myo.driver.MyoApplication;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;


/**
 * Created by paulwatson on 15/11/2014.
 */
public class MyoPresenter implements Initializable {

    private ObservableList<String> data;

    @FXML
    private Button button;

    @FXML
    private ListView<String> deviceList;

    @Inject
    private MyoApplication myo;

    @FXML
    private void onClick(MouseEvent evt) {
        System.out.println("******** foo *******");

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
}
