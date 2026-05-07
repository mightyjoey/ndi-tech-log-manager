package org.example.m11techlogapp.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.m11techlogapp.model.LogEntry;
import org.example.m11techlogapp.model.ConnectDB;
import org.example.m11techlogapp.model.LogEntryRepository;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.example.m11techlogapp.util.DateUtils.fromJulianToDateString;

public class searchController {

    @FXML
    public TextField textSearch;
    @FXML
    private TableView<LogEntry> tableView;


    public void search(javafx.event.ActionEvent actionEvent) {
        tableView.getItems().clear();
        tableView.getColumns().clear();

        String searchText = textSearch.getText();

        ConnectDB connectDB = new ConnectDB();
        LogEntryRepository logEntryRepository = new LogEntryRepository(connectDB);

        List<LogEntry> logEntries = logEntryRepository.searchForKeyword(searchText);
        connectDB.close();

        TableColumn<LogEntry, String> dateColumn = new TableColumn<>("DATE");
        dateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(fromJulianToDateString(cellData.getValue().getDttm())));

        TableColumn<LogEntry, String> nameColumn = new TableColumn<>("NAME");
        nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getName()));

        TableColumn<LogEntry, String> nomenColumn = new TableColumn<>("NOMEN");
        nomenColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getNomen()));

        TableColumn<LogEntry, String> malCdColumn = new TableColumn<>("MAL_CD");
        malCdColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getMal_cd()));

        TableColumn<LogEntry, String> hoursColumn = new TableColumn<>("HOURS");
        hoursColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getHours()));

        TableColumn<LogEntry, String> corrActColumn = new TableColumn<>("CORR_ACT");
        corrActColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getCorr_act()));

        // Add columns to the table
        tableView.getColumns().addAll(dateColumn, nameColumn, nomenColumn, malCdColumn, hoursColumn, corrActColumn);

        // Populate rows with LogEntry data
        ObservableList<LogEntry> observableList = FXCollections.observableArrayList(logEntries);
        tableView.setItems(observableList);
    }

    public void switchToMainPage(javafx.event.ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/org/example/m11techlogapp/mainPage.fxml")));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

}
