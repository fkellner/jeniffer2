package de.unituebingen.jeniffer2;

import de.unituebingen.jeniffer2.main.MainView;
import de.unituebingen.jeniffer2.main.modules.navigation.PathSaver;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * LEGAL NOTICE
 * 
 * This product includes DNG technology under license by Adobe.
 * Dieses Produkt enthaelt die bei Adobe lizenzierte DNG-Technologie.
 */
public class App extends Application {

    public static void main(String... args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Jeniffer2");
        stage.setMaximized(true);
        new PathSaver();
        MainView mainView = new MainView();
        Parent root = mainView.getView();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
}
