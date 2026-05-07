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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogEntryRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void insertEntryAndSearchForKeywordRoundTripThroughDatabase() {
        LogEntryRepository controller = controllerForTempDatabase();

        String result = controller.insertEntry(
                DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 15)),
                "JANE DOE",
                "WING PANEL",
                "572",
                1.5,
                "PERFORMED ET INSPECTION");

        assertEquals("update success", result);

        List<LogEntry> entries = controller.searchForKeyword("wing");

        assertEquals(1, entries.size());
        assertEquals("JANE DOE", entries.getFirst().getName());
        assertEquals("WING PANEL", entries.getFirst().getNomen());
    }

    @Test
    void getWorkerEntriesFiltersByNameDateRangeAndInspectionMethod() {
        LogEntryRepository controller = controllerForTempDatabase();
        double jan15 = DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 15));
        double jan20 = DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 20));
        double feb1 = DateUtils.fromDateToJulian(LocalDate.of(2024, 2, 1));

        controller.insertEntry(jan15, "JANE DOE", "WING PANEL", "572", 1.5, "VISUAL CHECK");
        controller.insertEntry(jan20, "JANE DOE", "TAIL PANEL", "0", 2.0, "PERFORMED EDDY CURRENT");
        controller.insertEntry(feb1, "JANE DOE", "DOOR PANEL", "576", 3.0, "PT CHECK");
        controller.insertEntry(jan15, "JOHN SMITH", "WING PANEL", "572", 4.0, "ET CHECK");

        List<LogEntry> entries = controller.getWorkerEntries(
                "Eddy Current",
                List.of("JANE DOE"),
                DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 1)),
                DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 31)));

        assertEquals(2, entries.size());
        assertTrue(entries.stream().allMatch(entry -> entry.getName().equals("JANE DOE")));
        assertTrue(entries.stream().anyMatch(entry -> entry.getNomen().equals("WING PANEL")));
        assertTrue(entries.stream().anyMatch(entry -> entry.getNomen().equals("TAIL PANEL")));
    }

    @Test
    void deleteEntryRemovesExactMatchingRecord() {
        LogEntryRepository controller = controllerForTempDatabase();
        double date = DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 15));

        controller.insertEntry(date, "JANE DOE", "WING PANEL", "572", 1.5, "ET CHECK");

        String result = controller.deleteEntry(date, "JANE DOE", "WING PANEL", "572", 1.5, "ET CHECK");

        assertEquals("delete success", result);
        assertTrue(controller.searchForKeyword("wing").isEmpty());
    }

    @Test
    void updateDB3ImportsValidExcelRows() throws IOException {
        LogEntryRepository controller = controllerForTempDatabase();
        Path workbookPath = tempDir.resolve("valid.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("1/15/2024 7:30");
            row.createCell(1).setCellValue("Jane Doe");
            row.createCell(2).setCellValue("Wing Panel");
            row.createCell(3).setCellValue("572");
            row.createCell(4).setCellValue(1.5);
            row.createCell(5).setCellValue("Performed ET inspection");

            try (OutputStream outputStream = Files.newOutputStream(workbookPath)) {
                workbook.write(outputStream);
            }
        }

        String result = controller.updateDB3(workbookPath.toString());

        assertEquals("Update Success: 1 entries updated out of 1", result);
        List<LogEntry> entries = controller.searchForKeyword("wing");
        assertEquals(1, entries.size());
        assertEquals("JANE DOE", entries.getFirst().getName());
        assertEquals("PERFORMED ET INSPECTION", entries.getFirst().getCorr_act());
    }

    @Test
    void updateDB3RollsBackWorkbookWithWrongColumnCount() throws IOException {
        LogEntryRepository controller = controllerForTempDatabase();
        Path workbookPath = tempDir.resolve("invalid.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row validRow = sheet.createRow(0);
            validRow.createCell(0).setCellValue("1/15/2024 7:30");
            validRow.createCell(1).setCellValue("Jane Doe");
            validRow.createCell(2).setCellValue("Wing Panel");
            validRow.createCell(3).setCellValue("572");
            validRow.createCell(4).setCellValue(1.5);
            validRow.createCell(5).setCellValue("Performed ET inspection");

            Row invalidRow = sheet.createRow(1);
            invalidRow.createCell(0).setCellValue("1/16/2024 7:30");
            invalidRow.createCell(1).setCellValue("Jane Doe");

            try (OutputStream outputStream = Files.newOutputStream(workbookPath)) {
                workbook.write(outputStream);
            }
        }

        String result = controller.updateDB3(workbookPath.toString());

        assertEquals(
                "table row must have exactly 6 columns (date time, name, nomenclature, mal. code, hours, corrective action)",
                result);
        assertTrue(controller.searchForKeyword("wing").isEmpty());
    }

    private LogEntryRepository controllerForTempDatabase() {
        Path dbPath = tempDir.resolve("worker_entry.db");
        ConnectDB connectDB = new ConnectDB("jdbc:sqlite:" + dbPath);
        return new LogEntryRepository(connectDB);
    }
}
