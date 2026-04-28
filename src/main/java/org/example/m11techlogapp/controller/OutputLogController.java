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
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.example.m11techlogapp.LogEntry;
import org.example.m11techlogapp.model.ConnectDB;
import org.example.m11techlogapp.model.DBController;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.*;

import static org.example.m11techlogapp.DateUtils.fromDateToJulian;
import static org.example.m11techlogapp.DateUtils.fromJulianToDateString;

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

    public void setAnnualLogStartDate(LocalDate annualLogStartDate) {
        this.annualLogStartDate = annualLogStartDate;
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
            DBController dbController = new DBController(connectDB);
            String output = dbController.deleteEntry(
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

    public void fillDetailedPDF(PDAcroForm acroForm) throws IOException {
        int count = 1;
        while (count < 11){
            PDField date = acroForm.getField("DATE_"+count);
            PDField name = acroForm.getField("NAME_"+count);
            PDField nomen = acroForm.getField("NOMEN_"+count);
            PDField hours = acroForm.getField("HOURS_"+count);
            PDField corrAct = acroForm.getField("undefined_"+count);

            if (!logEntries2.isEmpty()){
                LogEntry logEntry = logEntries2.poll();
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

    public void fillPDFSplitByYear(PDAcroForm acroForm, MonthDay splitDate) throws IOException {

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

        LocalDate firstRecordDate = LocalDate.parse(fromJulianToDateString(logEntries.getFirst().getDttm()));
        LocalDate periodStartDate = getAnnualPeriodStart(firstRecordDate, splitDate);
        LocalDate nextPeriodStartDate = periodStartDate.plusYears(1);

        int count = 1;
        String currentDate = "NA";
        while (count < 22){
            PDField date = acroForm.getField("6 DATERow"+count);
            PDField method = acroForm.getField("7 NDI METHODRow"+count);
            PDField hours = acroForm.getField("8 hoursRow"+count);
            PDField remarks = acroForm.getField("9 REMARKSrow"+count);

            if (!logEntries.isEmpty()){
                LocalDate nextEntryDate = LocalDate.parse(fromJulianToDateString(logEntries.peek().getDttm()));
                if (!nextEntryDate.isBefore(nextPeriodStartDate)) {
                    break;
                }

                LogEntry logEntry = logEntries.poll();
                currentDate = fromJulianToDateString(logEntry.getDttm());
                if (count == 1) {
                    PDField startDate = acroForm.getField("4 DATE IN");
                    startDate.setValue(currentDate);
                }
                date.setValue(currentDate);
                method.setValue(this.method);
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

    private LocalDate getAnnualPeriodStart(LocalDate entryDate, MonthDay splitDate) {
        LocalDate periodStart = splitDate.atYear(entryDate.getYear());
        if (entryDate.isBefore(periodStart)) {
            periodStart = splitDate.atYear(entryDate.getYear() - 1);
        }
        return periodStart;
    }

    private boolean isAllInspectionsLog() {
        return "All Inspections".equals(selectedMethod);
    }

    private Map<String, LinkedList<LogEntry>> groupByInspectionMethod(List<LogEntry> sourceEntries) {
        Map<String, LinkedList<LogEntry>> groupedEntries = new LinkedHashMap<>();
        groupedEntries.put("Eddy Current", new LinkedList<>());
        groupedEntries.put("Liquid Penetrant", new LinkedList<>());
        groupedEntries.put("Magnetic Particle", new LinkedList<>());
        groupedEntries.put("Radiographic", new LinkedList<>());
        groupedEntries.put("Ultrasonic", new LinkedList<>());
        groupedEntries.put("Other", new LinkedList<>());

        for (LogEntry entry : sourceEntries) {
            for (String inspectionMethod : getInspectionMethodsForEntry(entry)) {
                groupedEntries.get(inspectionMethod).add(entry);
            }
        }

        return groupedEntries;
    }

    private List<String> getInspectionMethodsForEntry(LogEntry entry) {
        return switch (entry.getMal_cd()) {
            case "572" -> List.of("Eddy Current");
            case "576" -> List.of("Liquid Penetrant");
            case "571" -> List.of("Magnetic Particle");
            case "570" -> List.of("Radiographic");
            case "575" -> List.of("Ultrasonic");
            default -> getInspectionMethodsFromCorrectiveAction(entry.getCorr_act());
        };
    }

    private List<String> getInspectionMethodsFromCorrectiveAction(String correctiveAction) {
        List<String> inspectionMethods = new ArrayList<>();
        String text = correctiveAction == null ? "" : " " + correctiveAction.toUpperCase() + " ";

        if (text.contains(" EDDY CURRENT") || text.contains(" ET ")) {
            inspectionMethods.add("Eddy Current");
        }
        if (text.contains(" PENETRANT") || text.contains(" PT ")) {
            inspectionMethods.add("Liquid Penetrant");
        }
        if (text.contains(" MAG PARTICLE") || text.contains(" MT ")) {
            inspectionMethods.add("Magnetic Particle");
        }
        if (text.contains(" RADIOGRAPHIC") || text.contains(" RT ")) {
            inspectionMethods.add("Radiographic");
        }
        if (text.contains(" ULTRASONIC") || text.contains(" UT ")) {
            inspectionMethods.add("Ultrasonic");
        }

        if (inspectionMethods.isEmpty()) {
            inspectionMethods.add("Other");
        }

        return inspectionMethods;
    }

    public void fillPDF(PDAcroForm acroForm) throws IOException {
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
        startDate.setValue(fromJulianToDateString(logEntries.getFirst().getDttm()));

        int count = 1;
        String currentDate = "NA";
        while (count < 22){
            PDField date = acroForm.getField("6 DATERow"+count);
            PDField method = acroForm.getField("7 NDI METHODRow"+count);
            PDField hours = acroForm.getField("8 hoursRow"+count);
            PDField remarks = acroForm.getField("9 REMARKSrow"+count);

            if (!logEntries.isEmpty()){
                LogEntry logEntry = logEntries.poll();
                currentDate = fromJulianToDateString(logEntry.getDttm());
                date.setValue(currentDate);
                if (count == 21){
                    PDField endDate = acroForm.getField("4 DATE OUT");
                    endDate.setValue(currentDate);
                }
                method.setValue(this.method);
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

    public void addIndividualRecord(javafx.event.ActionEvent event) throws IOException {
        if (dttmField.getValue() == null || newNameField.getText().isEmpty() || nomenField.getText().isEmpty() || corrActField.getText().isEmpty() || malCdField.getValue()==null) {
            showAlert(Alert.AlertType.INFORMATION, "Error", "fill all fields");
            return;
        }
        double date = fromDateToJulian(dttmField.getValue());
        String name = newNameField.getText().toUpperCase();
        String nomen = nomenField.getText().toUpperCase();
        String mal = selectMethod(malCdField.getValue());
        double hours = hoursField.getValue();
        String corrAct = corrActField.getText().toUpperCase();

        ConnectDB connectDB = new ConnectDB();
        DBController dbController = new DBController(connectDB);
        String result  = dbController.insertEntry(date, name, nomen, mal, hours, corrAct);
        connectDB.close();

        showAlert(Alert.AlertType.INFORMATION, "Status", result);

        if (result.equals("update success")){
            LogEntry entry = new LogEntry(String.valueOf(date),name,nomen,mal,String.valueOf(hours),corrAct);
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

        // Create a PDFMergerUtility
        PDFMergerUtility merger = new PDFMergerUtility();
        File outputFile = new File(newFolder, newPdfName);
        merger.setDestinationFileName(outputFile.getAbsolutePath());

        int pageCount = 1;

        try {
            if (newPdfName.equals("NDI_Tech_Log.pdf") && isAllInspectionsLog()) {
                String originalMethod = this.method;
                LinkedList<LogEntry> originalLogEntries = this.logEntries;

                for (Map.Entry<String, LinkedList<LogEntry>> methodEntries : groupByInspectionMethod(logEntriesList).entrySet()) {
                    if (methodEntries.getValue().isEmpty()) {
                        continue;
                    }

                    this.method = methodEntries.getKey();
                    this.logEntries = methodEntries.getValue();

                    while (!this.logEntries.isEmpty()) {
                        try (InputStream templateStream = getClass().getClassLoader().getResourceAsStream(pdfSource);
                             PDDocument document = PDDocument.load(templateStream)) {

                            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

                            if (annualLogStartDate == null) {
                                fillPDF(acroForm);
                            } else {
                                fillPDFSplitByYear(acroForm, MonthDay.from(annualLogStartDate));
                            }

                            if (acroForm != null) {
                                acroForm.flatten();
                            }

                            File tempPage = File.createTempFile("page_" + pageCount++, ".pdf");
                            tempPage.deleteOnExit();
                            document.save(tempPage);
                            merger.addSource(tempPage);
                        }
                    }
                }

                this.method = originalMethod;
                this.logEntries = originalLogEntries;
                logEntriesList.clear();
            }

            while (!logEntriesList.isEmpty()) {

                // Load template from resources (classpath)
                try (InputStream templateStream = getClass().getClassLoader().getResourceAsStream(pdfSource);
                     PDDocument document = PDDocument.load(templateStream)) {

                    PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

                    if (newPdfName.equals("NDI_Tech_Log.pdf")) {
                        if (annualLogStartDate == null) {
                            fillPDF(acroForm);
                        } else {
                            fillPDFSplitByYear(acroForm, MonthDay.from(annualLogStartDate));
                        }
                    } else {
                        fillDetailedPDF(acroForm);
                    }

                    if (acroForm != null) {
                        acroForm.flatten();
                    }

                    // Write the filled PDF to a temporary file
                    File tempPage = File.createTempFile("page_" + pageCount++, ".pdf");
                    tempPage.deleteOnExit();
                    document.save(tempPage);

                    // Add to the merger
                    merger.addSource(tempPage);
                }
            }

            // Merge all filled PDFs into one
            merger.mergeDocuments(null);
            System.out.println("Saved merged PDF: " + outputFile.getAbsolutePath());

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
