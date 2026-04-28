package org.example.m11techlogapp.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.m11techlogapp.model.DBController;
import org.example.m11techlogapp.model.ConnectDB;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import static org.example.m11techlogapp.DateUtils.fromDateToJulian;

public class UpdateRecordsController {

    @FXML
    private DatePicker dttmField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField nomenField;
    @FXML
    private ComboBox<String> malCdField;
    @FXML
    private Spinner<Double> hoursField;
    @FXML
    private TextField corrActField;


    private Stage stage;

    public void switchToMainPage(javafx.event.ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/org/example/m11techlogapp/mainPage.fxml")));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void clearDB(javafx.event.ActionEvent event) throws SQLException, IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete all records?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            ConnectDB connectDB = new ConnectDB();
            DBController dbController = new DBController(connectDB);
            dbController.clearDB();
            connectDB.close();

            Alert alert2 = new Alert(Alert.AlertType.INFORMATION);
            alert2.setTitle("Update Complete");
            alert2.setHeaderText(null);
            alert2.setContentText("database cleared");
            alert2.showAndWait();
        }
    }

    public void uploadFile(javafx.event.ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xls", "*.xlsx")
        );
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            ConnectDB connectDB = new ConnectDB();
            DBController dbController = new DBController(connectDB);
            String filePath = selectedFile.getAbsolutePath();
            String result = dbController.updateDB3(filePath);
            connectDB.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            if (result.equals("table row must have exactly 6 columns (date time, name, nomenclature, mal. code, hours, corrective action)")) {
                alert.setTitle("Update Failed");
            }else{
                alert.setTitle("Update Complete");
            }
            alert.setHeaderText(null);
            alert.setContentText(result);
            alert.showAndWait();

        } else {
            System.out.println("File selection cancelled.");
        }
    }

    public void addIndividualRecord(javafx.event.ActionEvent event) throws IOException {
        if (dttmField.getValue() == null || nameField.getText().isEmpty() || nomenField.getText().isEmpty() || corrActField.getText().isEmpty()|| malCdField.getValue()==null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Fill all fields");
            alert.showAndWait();
            return;
        }
        double date = fromDateToJulian(dttmField.getValue());
        String name = nameField.getText().toUpperCase();
        String nomen = nomenField.getText().toUpperCase();
        String mal = selectMethod(malCdField.getValue());
        double hours = hoursField.getValue();
        String corrAct = corrActField.getText().toUpperCase();

        ConnectDB connectDB = new ConnectDB();
        DBController dbController = new DBController(connectDB);

        String result  = dbController.insertEntry(date, name, nomen, mal, hours, corrAct);
        connectDB.close();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Update Complete");
        alert.setHeaderText(null);
        alert.setContentText(result);
        alert.showAndWait();

        if (result.equals("update success")){
            dttmField.setValue(null);
            nameField.setText(null);
            nomenField.setText(null);
            malCdField.setValue(null);
            corrActField.setText(null);
        }
    }

    public void initializeMalCd(MouseEvent event) {
        malCdField.setItems(FXCollections.observableArrayList("Eddy Current", "Liquid Penetrant", "Magnetic Particle", "Radiographic", "Ultrasonic", "0", "Other"));
    }

    private String selectMethod(String method) {
        return switch (method) {
            case "Eddy Current" -> "572";
            case "Liquid Penetrant" -> "576";
            case "Magnetic Particle" -> "571";
            case "Radiographic" -> "570";
            case "Ultrasonic" -> "575";
            case "Other" -> "579";
            default -> "0";
        };
    }


}
