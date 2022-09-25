package uk.gov.homeoffice.unhcr.cases.tool.gui;

import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.io.FilenameUtils;
import uk.gov.homeoffice.unhcr.cases.tool.CaseFileValidator;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CaseFileValidatorApplication extends Application {

    //Drag And Drop label is removed upon first file addiction
    private Label dragAndDropLabel = new Label("  DRAG & DROP\n  CASE FILES HERE");

    private ListView<CaseFileItem> caseFilesListView = new ListView<CaseFileItem>();

    public static class CaseFileItem {

        public File caseFile;
        public String label;

        public ValidationResult validationResult;

        private CaseFileItem() {}

        public static CaseFileItem ofFile(File caseFile) {
            CaseFileItem caseFileItem = new CaseFileItem();
            caseFileItem.caseFile = caseFile;
            caseFileItem.label = FilenameUtils.getName(caseFile.getPath());
            return caseFileItem;
        }

        public void setValidationResult(ValidationResult validationResult) {
            this.validationResult = validationResult;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static class CaseFileItemDecorator extends ListCell<CaseFileItem> {
        @Override
        protected void updateItem(CaseFileItem item, boolean empty) {
            super.updateItem(item, empty);

            String label = Optional.ofNullable(item)
                    .map(itemTmp -> itemTmp.label)
                    .orElse("");
            Color color = Optional.ofNullable(item)
                    .map(itemTmp -> itemTmp.validationResult)
                    .map(validationResult -> validationResult.isSuccess()?Color.GREEN:Color.RED)
                    .orElse(Color.BLACK);

            setText(label);
            setTextFill(color);
        }
    }

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

        caseFilesListView.setMinHeight(100);
        caseFilesListView.getItems().addListener((ListChangeListener<CaseFileItem>) change -> dragAndDropLabel.setVisible(caseFilesListView.getItems().isEmpty()));
        caseFilesListView.setCellFactory(new Callback<ListView<CaseFileItem>, ListCell<CaseFileItem>>() {
            @Override
            public ListCell<CaseFileItem> call(ListView<CaseFileItem> list) {
                return new CaseFileItemDecorator();
            }
        });


        VBox dragTargetCaseFilesList = new VBox();
        dragTargetCaseFilesList.getChildren().addAll(caseFilesListView);
        dragTargetCaseFilesList.setVgrow(caseFilesListView, Priority.ALWAYS);
        dragTargetCaseFilesList.setOnDragOver(event -> {
            if (
                    (event.getGestureSource() != dragTargetCaseFilesList) &&
                    (event.getDragboard().hasFiles())
            ) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        dragTargetCaseFilesList.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    addAndValidateFiles(db.getFiles());
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
        });



        TextArea validationResultText = new TextArea();
        validationResultText.setText("(validation results will appear here)");
        validationResultText.setMinHeight(200);

        Button addFileButton = new Button();
        addFileButton.setText("ADD FILE");
        addFileButton.setOnAction(event -> System.out.println("Hello World!"));
        addFileButton.setPrefWidth(100);

        Button clearButton = new Button();
        clearButton.setText("CLEAR");
        clearButton.setOnAction(event -> System.out.println("Hello World!"));
        clearButton.setPrefWidth(100);

        Button clearAllButton = new Button();
        clearAllButton.setText("CLEAR ALL");
        clearAllButton.setOnAction(event -> System.out.println("Hello World!"));
        clearAllButton.setPrefWidth(100);

        Button revalidateButton = new Button();
        revalidateButton.setText("RE-VALDIATE");
        revalidateButton.setOnAction(event -> System.out.println("Hello World!"));
        revalidateButton.setPrefWidth(100);

        Button exitButton = new Button();
        exitButton.setText("EXIT");
        exitButton.setOnAction(event -> System.out.println("Hello World!"));
        exitButton.setPrefWidth(100);

        FlowPane buttonsPane = new FlowPane(10, 10, addFileButton, clearButton, clearAllButton, revalidateButton, exitButton);
        buttonsPane.setPadding(new Insets(20,20,20,20));

        Label infoLabel = new Label("(drag & drop files into Case Files list; start application with -h to show all command line options)");
        infoLabel.setContentDisplay(ContentDisplay.CENTER);
        infoLabel.setStyle("-fx-border-color: lightgray;");

        GridPane root = new GridPane();
        root.add(dragTargetCaseFilesList, 0, 0 );
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

    private List<CaseFileItem> findCaseFileItems(File caseFile) {
        return caseFilesListView.getItems().stream()
                .filter(caseFileItem -> {
                    try {
                        return caseFileItem.caseFile.getCanonicalPath().equals(caseFile.getCanonicalPath());
                    } catch (IOException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    private void addAndValidateFiles(List<File> caseFiles) {
        System.out.println(String.format("Adding/validating files: %s", caseFiles.stream().map(file -> Objects.toString(file)).collect(Collectors.joining("."))));

       CaseFileValidator caseFileValidator = new CaseFileValidator();

        for (File caseFile : caseFiles) {
            if (!caseFile.isFile()) continue;

            ValidationResult validationResult;
            try {
                validationResult = caseFileValidator.validate(caseFile);
            } catch (IOException exception) {
                validationResult = new ValidationResult();
                validationResult.addError(exception.getMessage());
            }
            validationResult.setFileName(caseFile.getPath());

            List<CaseFileItem> caseFileItems = findCaseFileItems(caseFile);
            if (caseFileItems.isEmpty()) {
                //add new
                caseFileItems.add(CaseFileItem.ofFile(caseFile));
            }

            //update validation results
            for (CaseFileItem caseFileItem : caseFileItems) {
                caseFileItem.setValidationResult(validationResult);
            }
        }

        //update list
        caseFilesListView.refresh();

    }
}
