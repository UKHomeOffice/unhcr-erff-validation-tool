package uk.gov.homeoffice.unhcr.cases.tool.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import uk.gov.homeoffice.unhcr.cases.tool.CaseFileValidator;

import javax.swing.*;
import java.io.File;
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

        ListView<File> caseFilesList = new ListView<File>();
//        ObservableList<String> items = FXCollections.observableArrayList (
//                "Single", "Double", "Suite", "Family App", "Single", "Double", "Suite", "Family App", "Single", "Double", "Suite", "Family App", "Single", "Double", "Suite", "Family App", "Single", "Double", "Suite", "Family App","Single", "Double", "Suite", "Family App","Single", "Double", "Suite", "Family App","Single", "Double", "Suite", "Family App","Single", "Double", "Suite", "Family App","Single", "Double", "Suite", "Family App");
//        caseFilesList.setItems(items);
        caseFilesList.setMinHeight(100);

        //Drag And Drop label is removed upon first file addiction
        Label dragAndDropLabel = new Label("DRAG & DROP\nCASE FILES HERE");
        dragAndDropLabel.setContentDisplay(ContentDisplay.CENTER);

        TextArea validationResultText = new TextArea();
        validationResultText.setText("(validation results)");
        validationResultText.setMinHeight(200);

        Button addButton = new Button();
        addButton.setText("ADD");
        addButton.setOnAction(event -> System.out.println("Hello World!"));
        addButton.setPrefWidth(80);

        Button clearButton = new Button();
        clearButton.setText("CLEAR");
        clearButton.setOnAction(event -> System.out.println("Hello World!"));
        clearButton.setPrefWidth(80);

        Button clearAllButton = new Button();
        clearAllButton.setText("CLEAR ALL");
        clearAllButton.setOnAction(event -> System.out.println("Hello World!"));
        clearAllButton.setPrefWidth(80);

        Button revalidateButton = new Button();
        revalidateButton.setText("RE-VALDIATE");
        revalidateButton.setOnAction(event -> System.out.println("Hello World!"));
        revalidateButton.setPrefWidth(80);

        Button exitButton = new Button();
        exitButton.setText("EXIT");
        exitButton.setOnAction(event -> System.out.println("Hello World!"));
        exitButton.setPrefWidth(80);

        FlowPane buttonsPane = new FlowPane(10, 10, addButton, clearButton, clearAllButton, revalidateButton, exitButton);
        buttonsPane.setPadding(new Insets(20,20,20,20));

        Label infoLabel = new Label("(drag & drop files into Case Files list; start application with -h to show all command line options)");
        infoLabel.setContentDisplay(ContentDisplay.CENTER);
        infoLabel.setStyle("-fx-border-color: lightgray;");

        GridPane root = new GridPane();
        root.add(caseFilesList, 0, 0 );
        root.add(dragAndDropLabel, 0, 0 );
        root.add(validationResultText, 1, 0 );
        root.add(buttonsPane, 0, 1, 2, 1 );
        root.add(infoLabel, 0, 2, 2, 1 );

        RowConstraints growingRow = new RowConstraints();
        growingRow.setVgrow(Priority.ALWAYS);

        ColumnConstraints fixedColumn = new ColumnConstraints();
        fixedColumn.setHgrow(Priority.SOMETIMES);

        ColumnConstraints growingColumn = new ColumnConstraints();
        growingColumn.setHgrow(Priority.ALWAYS);

        root.getRowConstraints().addAll(growingRow);
        root.getColumnConstraints().addAll(fixedColumn,growingColumn);



        primaryStage.setScene(new Scene(root, 640, 480));
        primaryStage.setMinHeight(480);
        primaryStage.setMinWidth(640);
        primaryStage.show();
    }

}
