package org.example.m11techlogapp.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.example.m11techlogapp.model.ConnectDB;
import org.example.m11techlogapp.model.InspectionMethod;
import org.example.m11techlogapp.model.LogEntry;
import org.example.m11techlogapp.model.LogEntryRepository;
import org.example.m11techlogapp.model.WorkerAliasRepository;
import org.example.m11techlogapp.model.WorkerRepository;
import static org.example.m11techlogapp.util.DateUtils.fromDateToJulian;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class GenerateLogsController {

    @FXML
    private ListView<String> listView;
    @FXML
    private ListView<String> selectedNames;
    @FXML
    private ComboBox<String> comboBox;
    @FXML
    private DatePicker beginDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private CheckBox annualSplitCheckBox;
    @FXML
    private DatePicker anniversaryDatePicker;
    @FXML
    private RadioButton useWorkerRadioButton;
    @FXML
    private ListView<String> workerListView;
    @FXML
    private ListView<String> workerAliasListView;

    public void switchToMainPage(javafx.event.ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/org/example/m11techlogapp/mainPage.fxml")));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void initSelectedNames(ArrayList<String> distinctNames) {
        selectedNames.getItems().setAll(distinctNames);
    }

    public void initFormState(String method, java.time.LocalDate beginDate, java.time.LocalDate endDate, boolean splitAnnually, java.time.LocalDate anniversaryDate) {
        initializeComboBox(null);
        comboBox.setValue(method);
        beginDatePicker.setValue(beginDate);
        endDatePicker.setValue(endDate);
        annualSplitCheckBox.setSelected(splitAnnually);
        anniversaryDatePicker.setDisable(!splitAnnually);
        anniversaryDatePicker.setValue(anniversaryDate);
    }

    public void initAllNames(){
        ConnectDB connectDB = new ConnectDB();
        LogEntryRepository logEntryRepository = new LogEntryRepository(connectDB);
        ArrayList<String> names = logEntryRepository.selectDistinctName();
        connectDB.close();
        Collections.sort(names);
        listView.getItems().setAll(names);
        listView.getItems().removeAll(selectedNames.getItems());
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    public void initWorkerNames(){
        ConnectDB connectDB = new ConnectDB();
        WorkerRepository workerRepository = new WorkerRepository(connectDB);
        ArrayList<String> names = workerRepository.getAllWorkers();
        connectDB.close();
        Collections.sort(names);
        workerListView.getItems().setAll(names);
    }

    public void initWorkerAliasNames(String full_name){
        ConnectDB connectdDB = new ConnectDB();
        WorkerAliasRepository workerAliasRepository = new WorkerAliasRepository(connectdDB);
        List<String> names = workerAliasRepository.getAliasesForWorker(full_name);
        connectdDB.close();
        Collections.sort(names);
        workerAliasListView.getItems().setAll(names);
    }

    // Show the worker panels instead of the record-name panels when the radio is selected.
    public void isUseworkerSelected(javafx.event.ActionEvent event) {
        boolean isSelected = useWorkerRadioButton.isSelected();

        listView.setVisible(!isSelected);
        listView.setManaged(!isSelected);
        selectedNames.setVisible(!isSelected);
        selectedNames.setManaged(!isSelected);

        workerListView.setVisible(isSelected);
        workerListView.setManaged(isSelected);
        workerAliasListView.setVisible(isSelected);
        workerAliasListView.setManaged(isSelected);

        if (isSelected && workerListView.getItems().isEmpty()) {
            initWorkerNames();
        }
    }

    //    populate inspection type drop down menu
    public void initializeComboBox(MouseEvent event) {
        comboBox.setItems(FXCollections.observableArrayList(InspectionMethod.logMethodOptions()));
    }

    public void toggleAnnualSplit(javafx.event.ActionEvent event) {
        boolean splitAnnually = annualSplitCheckBox.isSelected();
        anniversaryDatePicker.setDisable(!splitAnnually);

        if (splitAnnually && anniversaryDatePicker.getValue() == null) {
            anniversaryDatePicker.setValue(beginDatePicker.getValue());
        } else if (!splitAnnually) {
            anniversaryDatePicker.setValue(null);
        }
    }

    //  take values from menu to create log and launch output log page
    public void generateLog(javafx.event.ActionEvent event) throws IOException, SQLException {
        String method = comboBox.getValue();
        ArrayList<String> nameList;
        if (useWorkerRadioButton.isSelected()){
            nameList = new ArrayList<>(workerAliasListView.getItems());
        } else {
            nameList = new ArrayList<>(selectedNames.getItems());
        }

        if (beginDatePicker.getValue() == null || endDatePicker.getValue() == null || comboBox.getValue() == null || nameList.isEmpty() || method == null) {
            showAlert(Alert.AlertType.INFORMATION, "Error", "fill all the fields");
            return;
        }
        double beginDate = fromDateToJulian(beginDatePicker.getValue());//test
        double endDate = fromDateToJulian(endDatePicker.getValue());//test

        if (beginDate > endDate) {
            showAlert(Alert.AlertType.INFORMATION, "Error", "beginning date is after end date");
            return;
        }

        if (annualSplitCheckBox.isSelected() && anniversaryDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.INFORMATION, "Error", "select an anniversary date or uncheck annual split");
            return;
        }

        // Fetch the log entries using the updated method
        ConnectDB connectDB = new ConnectDB();
        LogEntryRepository logEntryRepository = new LogEntryRepository(connectDB);
        List<LogEntry> logEntries = logEntryRepository.getWorkerEntries(method, nameList, beginDate, endDate);
        connectDB.close();

        // Load the FXML and pass the data to the controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/m11techlogapp/outputLogs.fxml"));
        Parent root = loader.load();
        OutputLogController controller = loader.getController();
        controller.initTable(logEntries,method);  // Pass the list of LogEntry objects
        controller.setGenerateLogState(beginDatePicker.getValue(), endDatePicker.getValue(), annualSplitCheckBox.isSelected(), anniversaryDatePicker.getValue());
        controller.getTotalHoursForLabel();

        controller.trackNames(nameList);

        // Switch to the new scene
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleMouseClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            ObservableList<String> selectedItems = listView.getSelectionModel().getSelectedItems();
            selectedNames.getItems().addAll(selectedItems);
            listView.getItems().removeAll(selectedItems);
        }
    }

    @FXML
    public void handleSelectedNamesClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            ObservableList<String> selected = FXCollections.observableArrayList(
                    selectedNames.getSelectionModel().getSelectedItems()
            );

            for (String name : selected) {
                if (!listView.getItems().contains(name)) {
                    listView.getItems().add(name);
                }
            }
            selectedNames.getItems().removeAll(selected);
            FXCollections.sort(listView.getItems());
        }
    }

    @FXML
    private void handleWorkerClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            String worker = workerListView.getSelectionModel().getSelectedItem();
            if (worker != null) {
                initWorkerAliasNames(worker);
            }
        }
    }

    @FXML
    private void handleWorkerAliasClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            String alias = workerAliasListView.getSelectionModel().getSelectedItem();
            if (alias != null) {
                workerAliasListView.getItems().remove(alias);
            }
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
