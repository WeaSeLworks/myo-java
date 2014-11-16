package com.github.weaselworks.ui;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.FXMLView;
import com.github.weaselworks.myo.driver.BluetoothClientFactory;
import com.github.weaselworks.myo.driver.MyoApplication;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MyoAppUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        //FXMLLoader loader = new FXMLLoader();
        MyoView appView = new MyoView();
        Scene scene = new Scene(appView.getView());
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        Injector.forgetAll();
        BluetoothClientFactory.instance().disconnect();
    }


}
