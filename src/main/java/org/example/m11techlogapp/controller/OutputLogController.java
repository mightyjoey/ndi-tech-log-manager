package org.example.m11techlogapp.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.example.m11techlogapp.util.AnnualLogPeriod;
import org.example.m11techlogapp.model.InspectionMethod;
import org.example.m11techlogapp.model.LogEntry;
import org.example.m11techlogapp.service.LogEntryService;
import org.example.m11techlogapp.service.PdfLogExporter;
import org.example.m11techlogapp.model.ConnectDB;
import org.example.m11techlogapp.model.LogEntryRepository;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.*;

import static org.example.m11techlogapp.util.DateUtils.fromJulianToDateString;

public class OutputLogController {

    @FXML
    public DatePicker dttmField;
    @FXML
    public TextField newNameField;
    @FXML
    public TextField nomenField;
    @FXML
    public ComboBox<String> malCdField;
    @FXML
    public TextField corrActField;
    @FXML
    public Spinner<Double> hoursField;
    @FXML
    private TableView<LogEntry> tableView;
    @FXML
    private Label totalHoursLabel;
    @FXML
    private TextField nameField;
    @FXML
    private TextField rankField;
    @FXML
    private TextField organizationField;

    private ArrayList<String> selectedNames;
    private List<LogEntry> entries;
    private LinkedList<LogEntry> logEntries;
    private LinkedList<LogEntry> logEntries2;
    private String method;
    private String selectedMethod;
    private LocalDate generateBeginDate;
    private LocalDate generateEndDate;
    private boolean annualSplitSelected;
    private LocalDate annualLogStartDate;

    public void getTotalHoursForLabel(){
        double total = 0;
        String totalInspections = String.valueOf(tableView.getItems().size());
        for (LogEntry entry: tableView.getItems()) {
            total+= Double.parseDouble(entry.getHours());
        }
        String totalHours = String.format("%.1f", total);
        totalHoursLabel.setText("Total Inspections Performed: " + totalInspections+ "    Total Hours: " + totalHours + "   (double click row to remove from results)");
    }

    public void trackNames(ArrayList<String> names){
        this.selectedNames = names;
    }

    public void setGenerateLogState(LocalDate beginDate, LocalDate endDate, boolean splitAnnually, LocalDate anniversaryDate) {
        this.generateBeginDate = beginDate;
        this.generateEndDate = endDate;
        this.annualSplitSelected = splitAnnually;
        this.annualLogStartDate = anniversaryDate;
    }

    //  create table and populate it
    public void initTable(List<LogEntry> logEntries, String method) {
        this.entries = logEntries;
        this.logEntries = new LinkedList<>(logEntries);
        this.logEntries2 = new LinkedList<>(logEntries);
        this.method = method;
        this.selectedMethod = method;
        tableView.getColumns().clear();
        tableView.getItems().clear();

        // Dynamically add columns based on LogEntry properties
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

        // Add double-click event on rows to remove them
        tableView.setRowFactory(_ -> {
            TableRow<LogEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    LogEntry rowData = row.getItem();
                    removeRow(rowData);
                }
            });
            return row;
        });

        // Update total hours label initially
        getTotalHoursForLabel();
    }

    // Method to remove a row from TableView and underlying lists
    private void removeRow(LogEntry entry) {
        tableView.getItems().remove(entry);
        logEntries.remove(entry);
        logEntries2.remove(entry);
        entries.remove(entry);
        getTotalHoursForLabel();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Do you want to remove this entry from the database as well?");
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesButton, noButton);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.get() == yesButton) {
            ConnectDB connectDB = new ConnectDB();
            LogEntryRepository logEntryRepository = new LogEntryRepository(connectDB);
            String output = logEntryRepository.deleteEntry(
                    Double.parseDouble(entry.getDttm()),
                    entry.getName(),
                    entry.getNomen(),
                    entry.getMal_cd(),
                    Double.parseDouble(entry.getHours()),
                    entry.getCorr_act());
            connectDB.close();

            showAlert(Alert.AlertType.INFORMATION, "Status", output);
        }
    }

    public void switchToGenerateLogs(javafx.event.ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/m11techlogapp/generateLogs.fxml"));
        Parent root = loader.load();
        GenerateLogsController controller = loader.getController();
        controller.initSelectedNames(selectedNames);
        controller.initAllNames();
        controller.initFormState(selectedMethod, generateBeginDate, generateEndDate, annualSplitSelected, annualLogStartDate);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void exportPDF(javafx.event.ActionEvent event) throws IOException {
        // Step 1: Ask user to select parent directory
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Parent Directory to Create Folder");
        File parentDir = directoryChooser.showDialog(((Node) event.getSource()).getScene().getWindow());

        if (parentDir == null) {
            System.out.println("No directory selected.");
            return;
        }

        // Step 2: Prompt for folder name
        TextInputDialog dialog = new TextInputDialog("NewLogFolder");
        dialog.setTitle("Enter Folder Name");
        dialog.setHeaderText("Please enter a name for the new folder to save the PDF:");
        dialog.setContentText("Folder name:");
        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) {
            System.out.println("No folder name entered.");
            return;
        }

        String newFolderName = result.get().trim();
        File newFolder = new File(parentDir, newFolderName);
        if (!newFolder.exists() && !newFolder.mkdirs()) {
            System.out.println("Failed to create folder: " + newFolder.getAbsolutePath());
            return;
        }

        exportFile(newFolder, "NDI_Tech_Log.pdf", logEntries, "ndiLog.pdf");
        exportFile(newFolder, "Detailed_Tech_Log.pdf", logEntries2, "detailedLog.pdf");
    }

    public void fillDetailedPDF(PDAcroForm acroForm, LinkedList<LogEntry> entries) throws IOException {
        int count = 1;
        while (count < 11){
            PDField date = acroForm.getField("DATE_"+count);
            PDField name = acroForm.getField("NAME_"+count);
            PDField nomen = acroForm.getField("NOMEN_"+count);
            PDField hours = acroForm.getField("HOURS_"+count);
            PDField corrAct = acroForm.getField("undefined_"+count);

            if (!entries.isEmpty()){
                LogEntry logEntry = entries.poll();
                date.setValue(fromJulianToDateString(logEntry.getDttm()));
                name.setValue(logEntry.getName());
                nomen.setValue(logEntry.getNomen());
                hours.setValue(logEntry.getHours());
                corrAct.setValue(logEntry.getCorr_act());
            }else{
                break;
            }
            count++;
        }
    }

    public void fillDetailedPDFSplitByYear(PDAcroForm acroForm, LinkedList<LogEntry> entries, MonthDay splitDate) throws IOException {
        LocalDate firstRecordDate = LocalDate.parse(fromJulianToDateString(entries.getFirst().getDttm()));
        LocalDate periodStartDate = AnnualLogPeriod.startFor(firstRecordDate, splitDate);
        LocalDate nextPeriodStartDate = periodStartDate.plusYears(1);

        int count = 1;
        while (count < 11){
            PDField date = acroForm.getField("DATE_"+count);
            PDField name = acroForm.getField("NAME_"+count);
            PDField nomen = acroForm.getField("NOMEN_"+count);
            PDField hours = acroForm.getField("HOURS_"+count);
            PDField corrAct = acroForm.getField("undefined_"+count);

            if (!entries.isEmpty()){
                LocalDate nextEntryDate = LocalDate.parse(fromJulianToDateString(entries.peek().getDttm()));
                if (!nextEntryDate.isBefore(nextPeriodStartDate)) {
                    break;
                }

                LogEntry logEntry = entries.poll();
                date.setValue(fromJulianToDateString(logEntry.getDttm()));
                name.setValue(logEntry.getName());
                nomen.setValue(logEntry.getNomen());
                hours.setValue(logEntry.getHours());
                corrAct.setValue(logEntry.getCorr_act());
            }else{
                break;
            }
            count++;
        }
    }

    public void fillPDFSplitByYear(PDAcroForm acroForm, LinkedList<LogEntry> entries, String methodName, MonthDay splitDate) throws IOException {

        PDField name = acroForm.getField("1 NAME");
        if (!nameField.getText().isEmpty()) {
            name.setValue(nameField.getText());
        }else{ name.setValue(""); }

        PDField rank = acroForm.getField("2 RANK");
        if (!rankField.getText().isEmpty()) {
            rank.setValue(rankField.getText());
        }else{ rank.setValue(""); }

        PDField organization = acroForm.getField("3 ORGANIZATION");
        if (!organizationField.getText().isEmpty()) {
            organization.setValue(organizationField.getText());
        }else{ organization.setValue(""); }

        LocalDate firstRecordDate = LocalDate.parse(fromJulianToDateString(entries.getFirst().getDttm()));
        LocalDate periodStartDate = AnnualLogPeriod.startFor(firstRecordDate, splitDate);
        LocalDate nextPeriodStartDate = periodStartDate.plusYears(1);

        int count = 1;
        String currentDate = "NA";
        while (count < 22){
            PDField date = acroForm.getField("6 DATERow"+count);
            PDField method = acroForm.getField("7 NDI METHODRow"+count);
            PDField hours = acroForm.getField("8 hoursRow"+count);
            PDField remarks = acroForm.getField("9 REMARKSrow"+count);

            if (!entries.isEmpty()){
                LocalDate nextEntryDate = LocalDate.parse(fromJulianToDateString(entries.peek().getDttm()));
                if (!nextEntryDate.isBefore(nextPeriodStartDate)) {
                    break;
                }

                LogEntry logEntry = entries.poll();
                currentDate = fromJulianToDateString(logEntry.getDttm());
                if (count == 1) {
                    PDField startDate = acroForm.getField("4 DATE IN");
                    startDate.setValue(currentDate);
                }
                date.setValue(currentDate);
                method.setValue(methodName);
                hours.setValue(logEntry.getHours());
                remarks.setValue(logEntry.getNomen());
            }else{
                break;
            }
            count++;
        }

        PDField endDate = acroForm.getField("4 DATE OUT");
        endDate.setValue(currentDate);
    }

    private boolean isAllInspectionsLog() {
        return InspectionMethod.isAllInspections(selectedMethod);
    }

    public void fillPDF(PDAcroForm acroForm, LinkedList<LogEntry> entries, String methodName) throws IOException {
        PDField name = acroForm.getField("1 NAME");
        if (!nameField.getText().isEmpty()) {
            name.setValue(nameField.getText());
        }else{ name.setValue(""); }

        PDField rank = acroForm.getField("2 RANK");
        if (!rankField.getText().isEmpty()) {
            rank.setValue(rankField.getText());
        }else{ rank.setValue(""); }

        PDField organization = acroForm.getField("3 ORGANIZATION");
        if (!organizationField.getText().isEmpty()) {
            organization.setValue(organizationField.getText());
        }else{ organization.setValue(""); }

        PDField startDate = acroForm.getField("4 DATE IN");
        startDate.setValue(fromJulianToDateString(entries.getFirst().getDttm()));

        int count = 1;
        String currentDate = "NA";
        while (count < 22){
            PDField date = acroForm.getField("6 DATERow"+count);
            PDField method = acroForm.getField("7 NDI METHODRow"+count);
            PDField hours = acroForm.getField("8 hoursRow"+count);
            PDField remarks = acroForm.getField("9 REMARKSrow"+count);

            if (!entries.isEmpty()){
                LogEntry logEntry = entries.poll();
                currentDate = fromJulianToDateString(logEntry.getDttm());
                date.setValue(currentDate);
                if (count == 21){
                    PDField endDate = acroForm.getField("4 DATE OUT");
                    endDate.setValue(currentDate);
                }
                method.setValue(methodName);
                hours.setValue(logEntry.getHours());
                remarks.setValue(logEntry.getNomen());
            }else{
                PDField endDate = acroForm.getField("4 DATE OUT");
                endDate.setValue(currentDate);
                break;
            }
            count++;
        }
    }

    public void initializeMalCd(MouseEvent event) {
        malCdField.setItems(FXCollections.observableArrayList(InspectionMethod.recordMethodOptions()));
    }

    public void addIndividualRecord(javafx.event.ActionEvent event) throws IOException {
        if (dttmField.getValue() == null || newNameField.getText().isEmpty() || nomenField.getText().isEmpty() || corrActField.getText().isEmpty() || malCdField.getValue()==null) {
            showAlert(Alert.AlertType.INFORMATION, "Error", "fill all fields");
            return;
        }
        LogEntryService.AddRecordResult result = new LogEntryService().addRecord(
                dttmField.getValue(),
                newNameField.getText(),
                nomenField.getText(),
                malCdField.getValue(),
                hoursField.getValue(),
                corrActField.getText());

        showAlert(Alert.AlertType.INFORMATION, "Status", result.message());

        if (result.isSuccess()){
            LogEntry entry = result.entry();
            ObservableList<LogEntry> observableList = tableView.getItems();
            observableList.add(entry);
            FXCollections.sort(observableList, Comparator.comparing(LogEntry::getDttm));

            entries.clear();
            entries.addAll(observableList);
            logEntries.clear();
            logEntries.addAll(observableList);
            logEntries2.clear();
            logEntries2.addAll(observableList);

            dttmField.setValue(null);
            newNameField.setText(null);
            nomenField.setText(null);
            malCdField.setValue(null);
            corrActField.setText(null);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void exportFile(File newFolder, String newPdfName, List<LogEntry> logEntriesList, String pdfSource) throws IOException {
        try {
            new PdfLogExporter().export(
                    newFolder,
                    newPdfName,
                    new ArrayList<>(logEntriesList),
                    pdfSource,
                    isAllInspectionsLog(),
                    method,
                    annualLogStartDate,
                    getClass(),
                    (acroForm, entries, methodName, splitDate) -> {
                        if (newPdfName.equals("NDI_Tech_Log.pdf")) {
                            if (splitDate == null) {
                                fillPDF(acroForm, entries, methodName);
                            } else {
                                fillPDFSplitByYear(acroForm, entries, methodName, splitDate);
                            }
                        } else if (splitDate == null) {
                            fillDetailedPDF(acroForm, entries);
                        } else {
                            fillDetailedPDFSplitByYear(acroForm, entries, splitDate);
                        }
                    });

            if (newPdfName.equals("NDI_Tech_Log.pdf")) {
                this.logEntries = new LinkedList<>(entries);
            } else {
                this.logEntries2 = new LinkedList<>(entries);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
