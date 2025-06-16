package com.amremad719.mosaicify;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.io.IOException;

/**
 * The {@code Mosaicify} class serves as the main entry point for the Mosaicify application.
 * It initializes the JavaFX application and loads the main UI from the FXML file.
 * <p>
 * Additionally, it loads the OpenCV native library required for image processing operations.
 */
public class Mosaicify extends Application {

    // Static block to load the OpenCV native library when the class is loaded
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * Starts the JavaFX application by loading the main user interface defined in the FXML file.
     *
     * @param stage the primary stage for this application, onto which the application scene is set.
     * @throws IOException if the FXML file cannot be loaded.
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Mosaicify.class.getResource("Mosaicify-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Mosaicify");

        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * The main method which launches the JavaFX application.
     *
     * @param args command-line arguments passed to the application (not used).
     */
    public static void main(String[] args) {
        launch();
    }
}
