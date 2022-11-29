package uk.gov.homeoffice.unhcr.cases.tool.gui;

import com.google.common.collect.Lists;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import uk.gov.homeoffice.unhcr.cases.tool.CaseFileValidator;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;
import uk.gov.homeoffice.unhcr.config.ConfigProperties;
import uk.gov.homeoffice.unhcr.version.GitHubVersionChecker;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CaseFileValidatorApplication extends Application {

    //Drag And Drop label is removed upon first file addiction
    private Label dragAndDropLabel = new Label("  DRAG & DROP\n  CASE FILES HERE");

    private ListView<CaseFileItem> caseFilesListView = new ListView<CaseFileItem>();

    private CheckBox autoCheckNewerVersionCheckBox = new CheckBox();

    private TextArea validationResultText = new TextArea();

    private AtomicBoolean newVersionAlertShown = new AtomicBoolean();

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

        //load config, show error if any config value has errors
        boolean autoCheckNewerVersionFlag = false;
        try {
            autoCheckNewerVersionFlag = ConfigProperties.getConfigPropertyAsBoolean(ConfigProperties.AUTOCHECK_NEWER_VERSION, true);
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(
                        Alert.AlertType.ERROR,
                        String.format("Error loading config file:\n%s: %s\n\nDo you want to continue with default settings?", e.getClass().getSimpleName(), e.getMessage()),
                        ButtonType.NO, ButtonType.YES);
                alert.initModality(Modality.APPLICATION_MODAL);
                if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) {
                    Platform.exit();
                }
            });
        }

        caseFilesListView.setMinHeight(100);
        caseFilesListView.getItems().addListener((ListChangeListener<CaseFileItem>) change -> dragAndDropLabel.setVisible(caseFilesListView.getItems().isEmpty()));
        caseFilesListView.setCellFactory(list -> new CaseFileItemDecorator());
        caseFilesListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> showValidationResult(newValue));

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

        Button addFilesButton = new Button();
        addFilesButton.setText("ADD FILES");
        addFilesButton.setOnAction(event -> selectAndAddCaseFiles(primaryStage));
        addFilesButton.setPrefWidth(100);

        Button clearButton = new Button();
        clearButton.setText("CLEAR");
        clearButton.setOnAction(event -> clearSelectedCaseFiles());
        clearButton.setPrefWidth(100);

        Button clearAllButton = new Button();
        clearAllButton.setText("CLEAR ALL");
        clearAllButton.setOnAction(event -> clearAllCaseFiles());
        clearAllButton.setPrefWidth(100);

        Button revalidateButton = new Button();
        revalidateButton.setText("RE-VALIDATE");
        revalidateButton.setOnAction(event -> revalidateAllCaseFiles());
        revalidateButton.setPrefWidth(100);

        Button exitButton = new Button();
        exitButton.setText("EXIT");
        exitButton.setOnAction(event -> Platform.exit());
        exitButton.setPrefWidth(100);

        autoCheckNewerVersionCheckBox.setText("Auto-check newer\nversion (every 24 hrs)");
        autoCheckNewerVersionCheckBox.setSelected(autoCheckNewerVersionFlag);
        autoCheckNewerVersionCheckBox.setOnAction(event -> setAutoCheckNewerVersion(autoCheckNewerVersionCheckBox.isSelected()));
        autoCheckNewerVersionCheckBox.setPrefWidth(150);

        FlowPane buttonsPane = new FlowPane(10, 10, addFilesButton, clearButton, clearAllButton, revalidateButton, exitButton, autoCheckNewerVersionCheckBox);
        buttonsPane.setPadding(new Insets(20,20,20,20));

        Label infoLabel = new Label("  (drag & drop files into Case Files list; start application with -h to show all command line options)  ");
        infoLabel.setContentDisplay(ContentDisplay.CENTER);
        infoLabel.setStyle("-fx-border-color: lightgray;");

        GridPane root = new GridPane();
        root.add(dragTargetCaseFilesList, 0, 0 );
        root.add(dragAndDropLabel, 0, 0 );
        root.add(validationResultText, 1, 0 );
        root.add(buttonsPane, 0, 2, 2, 1 );
        root.add(infoLabel, 0, 3, 2, 1 );

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


        //auto-check version scheduler
        ScheduledService<Void> autoCheckNewerVersionService = new ScheduledService<Void>() {
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    protected Void call() {
                        checkNewerVersion();
                        return null;
                    }
                };
            }
        };
        autoCheckNewerVersionService.setDelay(Duration.seconds(5));
        autoCheckNewerVersionService.setPeriod(Duration.hours(24)); //repeat check every 24 hours
        autoCheckNewerVersionService.start();
    }

    private void checkNewerVersion() {
        final boolean autoCheckNewerVersionFlag = ConfigProperties.getConfigPropertyAsBoolean(ConfigProperties.AUTOCHECK_NEWER_VERSION, true);
        if (!autoCheckNewerVersionFlag) return;

        //only one version alert is visible
        if (newVersionAlertShown.getAndSet(true)) return;
        Platform.runLater(() -> {
            try {
                final boolean newerVersionFlag = GitHubVersionChecker.checkReleaseVersionNewer();
                final String currentVersion = Objects.toString(GitHubVersionChecker.getCurrentVersion(), "N/A");
                final String newerVersion = Objects.toString(GitHubVersionChecker.getLatestReleaseVersionCached(), "N/A");
                if (newerVersionFlag) {
                    System.out.println(String.format("Newer remote version found: %s (local version %s)", newerVersion, currentVersion));
                    Alert alert = new Alert(
                            Alert.AlertType.CONFIRMATION,
                            String.format("Newer version (%s) found at:\n%s\n\nDo you want to open page?", newerVersion, GitHubVersionChecker.GET_LATEST_VERSION_URL),
                            ButtonType.NO, ButtonType.YES);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        openUrl(GitHubVersionChecker.GET_LATEST_VERSION_URL);
                    }
                } else {
                    System.out.println(String.format("Remote version found: %s (local version %s)", newerVersion, currentVersion));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(
                        Alert.AlertType.ERROR,
                        String.format("Error checking for newer version:\n%s: %s\n\nDo you want to disable auto-check?", e.getClass().getSimpleName(), e.getMessage()), ButtonType.NO, ButtonType.YES);
                alert.initModality(Modality.APPLICATION_MODAL);
                if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                    autoCheckNewerVersionCheckBox.setSelected(false);
                    setAutoCheckNewerVersion(false);
                }
            } finally {
                newVersionAlertShown.set(false);
            }
        });
    }

    private void setAutoCheckNewerVersion(boolean autoCheckNewerVersionFlag) {
        try {
            ConfigProperties.setConfigProperty(ConfigProperties.AUTOCHECK_NEWER_VERSION, autoCheckNewerVersionFlag);
            if (autoCheckNewerVersionFlag) {
                checkNewerVersion();  //run immediately
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    String.format("Error saving config file:\n%s: %s", e.getClass().getSimpleName(), e.getMessage()),
                    ButtonType.CLOSE);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.showAndWait();
        }
    }

    private void openUrl(String url) throws Exception {
        if (ConfigProperties.isMacOSX()) {
            //workaround for Mac OS X
            //java.lang.ClassNotFoundException: com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory

            @SuppressWarnings("rawtypes") Class clazz = Class.forName("com.apple.eio.FileManager");
            @SuppressWarnings("rawtypes") Method openURL = clazz.getDeclaredMethod("openURL", new Class[] {String.class});
            openURL.invoke(null, new Object[] {url});
        } else {
            getHostServices().showDocument(url);
        }
    }

    private void selectAndAddCaseFiles(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add Case File");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Xml Files", "*.xml"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
        if (selectedFiles != null) {
            addAndValidateFiles(selectedFiles);
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
                .distinct()
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

        //refresh list
        caseFilesListView.refresh();
    }
}
