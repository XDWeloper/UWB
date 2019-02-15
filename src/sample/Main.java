package sample;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("1X бот");
        Scene scene = new Scene(root, 1200, 1000);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("Image/XDWelopLogo.png"));
        scene.getStylesheets().add((getClass().getResource("style.css")).toExternalForm());
        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Script._IsStop = true;
            }
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}
