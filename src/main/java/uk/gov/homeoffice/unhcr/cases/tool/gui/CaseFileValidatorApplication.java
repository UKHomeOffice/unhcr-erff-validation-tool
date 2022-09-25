package uk.gov.homeoffice.unhcr.cases.tool.gui;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import uk.gov.homeoffice.unhcr.cases.tool.CaseFileValidator;

import javax.swing.*;
import java.lang.management.ManagementFactory;

public class CaseFileValidatorApplication extends Application {

    public static void main(String[] args) {

        String jvmVersion = ManagementFactory.getRuntimeMXBean().getVmVersion();
        if (
                (jvmVersion.startsWith("1."))||
                (jvmVersion.startsWith("10"))
        ) {
            // use Swing message to present error
            JOptionPane.showMessageDialog(
                    null,
                    "To start GUI dialog, Java version 11 (or higher) is required.",
                    "ERROR",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(CaseFileValidator.NAME_AND_VERSION);

        Button btn = new Button();
        btn.setText("CLICK ME");
        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
            }
        });

        StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
    }

}
