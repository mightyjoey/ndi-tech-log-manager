package org.example.m11techlogapp.model;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.m11techlogapp.util.DateUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogEntryWorkflowIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void importsExcelThenSearchesAndFiltersGeneratedLogEntries() throws IOException {
        LogEntryRepository repository = repositoryForTempDatabase();
        Path workbookPath = tempDir.resolve("mixed-log.xlsx");

        writeWorkbook(
                workbookPath,
                row("1/15/2024 7:30", "Jane Doe", "Wing Panel", "572", 1.5, "Visual check"),
                row("1/20/2024 8:45", "Jane Doe", "Tail Panel", "0", 2.0, "Performed eddy current"),
                row("2/1/2024 9:15", "Jane Doe", "Door Panel", "576", 3.0, "Performed PT inspection"),
                row("1/15/2024 7:30", "John Smith", "Wing Panel", "572", 4.0, "ET check"));

        assertEquals("Update Success: 4 entries updated out of 4", repository.updateDB3(workbookPath.toString()));

        List<LogEntry> keywordResults = repository.searchForKeyword("wing");
        assertEquals(2, keywordResults.size());

        List<LogEntry> generatedLogEntries = repository.getWorkerEntries(
                "Eddy Current",
                List.of("JANE DOE"),
                DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 1)),
                DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 31)));

        assertEquals(2, generatedLogEntries.size());
        assertTrue(generatedLogEntries.stream().allMatch(entry -> entry.getName().equals("JANE DOE")));
        assertTrue(generatedLogEntries.stream().anyMatch(entry -> entry.getNomen().equals("WING PANEL")));
        assertTrue(generatedLogEntries.stream().anyMatch(entry -> entry.getNomen().equals("TAIL PANEL")));
    }

    @Test
    void ignoresDuplicatePrimaryKeyRowsAcrossManualAndExcelImports() throws IOException {
        LogEntryRepository repository = repositoryForTempDatabase();
        double date = DateUtils.fromDateTimeToJulian("1/15/2024 7:30");

        assertEquals("update success", repository.insertEntry(
                date,
                "JANE DOE",
                "WING PANEL",
                "572",
                1.5,
                "PERFORMED ET INSPECTION"));

        Path workbookPath = tempDir.resolve("duplicate.xlsx");
        writeWorkbook(
                workbookPath,
                row("1/15/2024 7:30", "Jane Doe", "Wing Panel", "572", 1.5, "Performed ET inspection"));

        assertEquals("Update Success: 0 entries updated out of 1", repository.updateDB3(workbookPath.toString()));
        assertEquals(1, repository.searchForKeyword("wing").size());
    }

    @Test
    void allInspectionsResultsCanBeGroupedByDetectedInspectionMethod() {
        LogEntryRepository repository = repositoryForTempDatabase();
        double jan15 = DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 15));

        repository.insertEntry(jan15, "JANE DOE", "WING PANEL", "572", 1.5, "VISUAL CHECK");
        repository.insertEntry(jan15, "JANE DOE", "TAIL PANEL", "0", 2.0, "PERFORMED PT AND UT INSPECTIONS");
        repository.insertEntry(jan15, "JANE DOE", "DOOR PANEL", "579", 0.5, "GENERAL REPAIR");

        List<LogEntry> allInspectionEntries = repository.getWorkerEntries(
                InspectionMethod.ALL_INSPECTIONS,
                List.of("JANE DOE"),
                DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 1)),
                DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 31)));

        Map<String, LinkedList<LogEntry>> groupedEntries = InspectionMethod.groupEntriesByMethod(allInspectionEntries);

        assertEquals(3, allInspectionEntries.size());
        assertEquals(1, groupedEntries.get("Eddy Current").size());
        assertEquals(1, groupedEntries.get("Liquid Penetrant").size());
        assertEquals(1, groupedEntries.get("Ultrasonic").size());
        assertEquals(1, groupedEntries.get("Other").size());
    }

    private LogEntryRepository repositoryForTempDatabase() {
        Path dbPath = tempDir.resolve("worker_entry.db");
        ConnectDB connectDB = new ConnectDB("jdbc:sqlite:" + dbPath);
        return new LogEntryRepository(connectDB);
    }

    private static Object[] row(String dateTime, String name, String nomen, String malCode, double hours, String correctiveAction) {
        return new Object[]{dateTime, name, nomen, malCode, hours, correctiveAction};
    }

    private static void writeWorkbook(Path workbookPath, Object[]... rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row sheetRow = sheet.createRow(rowIndex);
                Object[] values = rows[rowIndex];
                sheetRow.createCell(0).setCellValue((String) values[0]);
                sheetRow.createCell(1).setCellValue((String) values[1]);
                sheetRow.createCell(2).setCellValue((String) values[2]);
                sheetRow.createCell(3).setCellValue((String) values[3]);
                sheetRow.createCell(4).setCellValue((double) values[4]);
                sheetRow.createCell(5).setCellValue((String) values[5]);
            }

            try (OutputStream outputStream = Files.newOutputStream(workbookPath)) {
                workbook.write(outputStream);
            }
        }
    }
}
