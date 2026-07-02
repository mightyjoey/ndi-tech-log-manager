package org.example.m11techlogapp.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import org.example.m11techlogapp.model.ConnectDB;
import org.example.m11techlogapp.model.LogEntryRepository;
import org.example.m11techlogapp.model.WorkerAliasRepository;
import org.example.m11techlogapp.model.WorkerRepository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ManageWorkerController {
    @FXML
    private ListView<String> workerListView; // list of worker full names
    @FXML
    private ListView<String> logEntryNameListView; // list of names that appear in work records
    @FXML
    private TextField newWorkerField; // text field for adding a new worker full name
    @FXML
    private Text nameText; // currently selected worker name
    @FXML
    private ListView<String> aliasListView; // list of aliases for the selected worker

    public void switchToMainPage(javafx.event.ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/org/example/m11techlogapp/mainPage.fxml")));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // Initialize the ListView with all names that appear in work records
    public void initAllLogEntryNames(){
        ConnectDB connectDB = new ConnectDB();
        LogEntryRepository logEntryRepository = new LogEntryRepository(connectDB);
        ArrayList<String> names = logEntryRepository.selectDistinctName();
        connectDB.close();
        Collections.sort(names);
        logEntryNameListView.getItems().setAll(names);
        logEntryNameListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    // Initialize the ListView with all created worker full names
    public void initAllWorkers(){
        ConnectDB connectDB = new ConnectDB();
        WorkerRepository workerRepository = new WorkerRepository(connectDB);
        ArrayList<String> names = workerRepository.getAllWorkers();
        connectDB.close();
        Collections.sort(names);
        workerListView.getItems().setAll(names);
        workerListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    //create a new worker full name in the database and update the ListView
    public void createNewWorker(){
        String full_name = newWorkerField.getText();
        if (full_name != null && !full_name.isEmpty()){
            ConnectDB connectDB = new ConnectDB();
            WorkerRepository workerRepository = new WorkerRepository(connectDB);
            String result = workerRepository.addWorker(full_name);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Add New Worker");
            alert.setHeaderText(null);
            alert.setContentText(result);
            alert.showAndWait();
            connectDB.close();
            initAllWorkers();
        }
    }

    // set the text of the name Text element
    public void setNameText(MouseEvent event) {
        if (event.getClickCount() == 2) {

            String selectedName = workerListView.getSelectionModel().getSelectedItem();
            nameText.setText(selectedName);

            if (selectedName != null) {
                ConnectDB connectDB = new ConnectDB();
                WorkerAliasRepository workerAliasRepository = new WorkerAliasRepository(connectDB);
                ObservableList<String> aliases = FXCollections.observableArrayList(
                    workerAliasRepository.getAliasesForWorker(selectedName)
                );
                connectDB.close();
                aliasListView.setItems(aliases);
                aliasListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            }

        }
    }
    
    // add names from names that appear in work records to the list of created worker full names
    public void addAlias(MouseEvent event) {
        if (event.getClickCount() == 2) {
            String selectedAlias = logEntryNameListView.getSelectionModel().getSelectedItem();
            if (newWorkerField != null && 
                selectedAlias != null &&
                !aliasListView.getItems().contains(selectedAlias)) {
                aliasListView.getItems().add(selectedAlias);
            }
        }
    }

    // remove selected aliases from the list of aliases for the selected worker
    public void removeAlias(MouseEvent event) {
        if (event.getClickCount() == 2) {
            String selectedAlias = aliasListView.getSelectionModel().getSelectedItem();
            if (selectedAlias != null) {
                aliasListView.getItems().remove(selectedAlias);
            }
        }
    }

    // update the aliases for the selected worker in the database
    public void updateWorkerAliases(){
        ArrayList<String> selectedAliases = new ArrayList<>(aliasListView.getItems());
        String full_name = nameText.getText();
        ConnectDB connectDB = new ConnectDB();
        WorkerAliasRepository workerAliasRepository = new WorkerAliasRepository(connectDB);
        String result = workerAliasRepository.updateWorkerAliases(full_name, selectedAliases);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Update Worker Aliases");
        alert.setHeaderText(null);
        alert.setContentText(result);
        alert.showAndWait();
        connectDB.close();
    }


}
