package uk.gov.homeoffice.unhcr.cases.tool.gui;

import com.google.common.collect.Lists;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import uk.gov.homeoffice.unhcr.cases.tool.CaseFileValidator;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CaseFileValidatorApplication extends Application {

    //Drag And Drop label is removed upon first file addiction
    private Label dragAndDropLabel = new Label("  DRAG & DROP\n  CASE FILES HERE");

    private ListView<CaseFileItem> caseFilesListView = new ListView<CaseFileItem>();

    private TextArea validationResultText = new TextArea();

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
        caseFilesListView.setCellFactory(list -> new CaseFileItemDecorator());
        caseFilesListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            showValidationResult(newValue);
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

        validationResultText.setMinHeight(200);
        showValidationResult(null);

        Button addFileButton = new Button();
        addFileButton.setText("ADD FILE");
        addFileButton.setOnAction(event -> selectAndAddFile(primaryStage));
        addFileButton.setPrefWidth(100);

        Button clearButton = new Button();
        clearButton.setText("CLEAR");
        clearButton.setOnAction(event -> clearSelectedCaseFiles());
        clearButton.setPrefWidth(100);

        Button clearAllButton = new Button();
        clearAllButton.setText("CLEAR ALL");
        clearAllButton.setOnAction(event -> clearAllCaseFiles());
        clearAllButton.setPrefWidth(100);

        Button revalidateButton = new Button();
        revalidateButton.setText("RE-VALDIATE");
        revalidateButton.setOnAction(event -> revalidateAllCaseFiles());
        revalidateButton.setPrefWidth(100);

        Button exitButton = new Button();
        exitButton.setText("EXIT");
        exitButton.setOnAction(event -> Platform.exit());
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

    private void selectAndAddFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add Case File");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Xml Files", "*.xml"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            addAndValidateFiles(Arrays.asList(selectedFile));
        }

    }

    private void clearSelectedCaseFiles() {
        List<CaseFileItem> selectedCaseFileItems = caseFilesListView.getSelectionModel().getSelectedItems();
        caseFilesListView.getItems().removeAll(selectedCaseFileItems);
    }

    private void showValidationResult(CaseFileItem item) {
        String text = Optional.ofNullable(item)
                .map(itemTmp -> itemTmp.validationResult)
                .map(validationResult -> validationResult.toString())
                .orElse("(validation results will appear here)");

        validationResultText.setText(text);
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

    private void clearAllCaseFiles() {
        caseFilesListView.getItems().clear();
    }

    private void revalidateAllCaseFiles() {
        List<File> caseFiles = caseFilesListView.getItems().stream().map(item -> item.caseFile).collect(Collectors.toList());
        addAndValidateFiles(caseFiles);
    }

    private void addAndValidateFiles(List<File> caseFiles) {
        System.out.println(String.format("Adding/validating files: %s", caseFiles.stream().map(file -> Objects.toString(file)).collect(Collectors.joining("."))));

        CaseFileValidator caseFileValidator = new CaseFileValidator();

        // get all files in directories
        List<File> extraCaseFiles = caseFiles.stream()
                .filter(caseFile -> caseFile.isDirectory())
                .map(directory -> FileUtils.listFiles(directory, null, true))
                .flatMap(list -> list.stream())
                .collect(Collectors.toList());

        List<File> allCaseFiles = Lists.newLinkedList(caseFiles);
        allCaseFiles.addAll(extraCaseFiles);

        for (File caseFile : allCaseFiles) {
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
                CaseFileItem caseFileItem = CaseFileItem.ofFile(caseFile);
                //add new
                caseFileItems.add(caseFileItem);
                caseFilesListView.getItems().add(caseFileItem);
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
