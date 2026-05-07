package org.example.m11techlogapp.service;

import org.example.m11techlogapp.model.InspectionMethod;
import org.example.m11techlogapp.model.LogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfLogExporterIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void exportCreatesPdfFromBundledTemplate() throws Exception {
        List<LogEntry> entries = List.of(
                new LogEntry("2460325", "JANE DOE", "WING PANEL", "572", "1.5", "ET CHECK"),
                new LogEntry("2460330", "JANE DOE", "TAIL PANEL", "572", "2.0", "ET CHECK"));
        Path outputPath = tempDir.resolve("NDI_Tech_Log.pdf");

        new PdfLogExporter().export(
                tempDir.toFile(),
                outputPath.getFileName().toString(),
                entries,
                "ndiLog.pdf",
                false,
                "Eddy Current",
                null,
                getClass(),
                (acroForm, pageEntries, method, splitDate) -> pageEntries.clear());

        assertTrue(Files.exists(outputPath));
        assertTrue(Files.size(outputPath) > 0);
    }

    @Test
    void allInspectionsExportInvokesPageFillerForDetectedMethodGroups() throws Exception {
        List<LogEntry> entries = List.of(
                new LogEntry("2460325", "JANE DOE", "WING PANEL", "572", "1.5", "ET CHECK"),
                new LogEntry("2460330", "JANE DOE", "TAIL PANEL", "0", "2.0", "PT AND UT CHECK"),
                new LogEntry("2460335", "JANE DOE", "DOOR PANEL", "579", "0.5", "GENERAL REPAIR"));
        List<String> methodsSeen = new ArrayList<>();
        Path outputPath = tempDir.resolve("All_Inspections.pdf");

        new PdfLogExporter().export(
                tempDir.toFile(),
                outputPath.getFileName().toString(),
                entries,
                "ndiLog.pdf",
                true,
                InspectionMethod.ALL_INSPECTIONS,
                LocalDate.of(2024, 1, 1),
                getClass(),
                (acroForm, pageEntries, method, splitDate) -> {
                    methodsSeen.add(method);
                    drainOnePage(pageEntries);
                });

        assertTrue(Files.exists(outputPath));
        assertTrue(Files.size(outputPath) > 0);
        assertEquals(List.of("Eddy Current", "Liquid Penetrant", "Ultrasonic", "Other"), methodsSeen);
    }

    private static void drainOnePage(LinkedList<LogEntry> entries) {
        while (!entries.isEmpty()) {
            entries.poll();
        }
    }
}
