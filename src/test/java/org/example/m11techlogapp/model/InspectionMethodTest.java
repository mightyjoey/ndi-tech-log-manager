package org.example.m11techlogapp.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InspectionMethodTest {

    @Test
    void malCodeForDisplayNameMapsKnownMethodsAndDefaultsToZero() {
        assertEquals("572", InspectionMethod.malCodeForDisplayName("Eddy Current"));
        assertEquals("576", InspectionMethod.malCodeForDisplayName("Liquid Penetrant"));
        assertEquals("571", InspectionMethod.malCodeForDisplayName("Magnetic Particle"));
        assertEquals("570", InspectionMethod.malCodeForDisplayName("Radiographic"));
        assertEquals("575", InspectionMethod.malCodeForDisplayName("Ultrasonic"));
        assertEquals("579", InspectionMethod.malCodeForDisplayName("Other"));
        assertEquals("0", InspectionMethod.malCodeForDisplayName("0"));
    }

    @Test
    void methodNamesForEntryMapsKnownMalCodes() {
        assertEquals(List.of("Eddy Current"), InspectionMethod.methodNamesForEntry(entry("572", "")));
        assertEquals(List.of("Liquid Penetrant"), InspectionMethod.methodNamesForEntry(entry("576", "")));
        assertEquals(List.of("Magnetic Particle"), InspectionMethod.methodNamesForEntry(entry("571", "")));
        assertEquals(List.of("Radiographic"), InspectionMethod.methodNamesForEntry(entry("570", "")));
        assertEquals(List.of("Ultrasonic"), InspectionMethod.methodNamesForEntry(entry("575", "")));
    }

    @Test
    void methodNamesFromCorrectiveActionDetectsMultipleMethodsAndFallsBackToOther() {
        assertEquals(
                List.of("Eddy Current", "Radiographic"),
                InspectionMethod.methodNamesFromCorrectiveAction("completed ET and RT inspections"));
        assertEquals(List.of("Other"), InspectionMethod.methodNamesFromCorrectiveAction("general repair"));
        assertEquals(List.of("Other"), InspectionMethod.methodNamesFromCorrectiveAction(null));
    }

    @Test
    void groupEntriesByMethodPlacesEntriesIntoExpectedBuckets() {
        List<LogEntry> entries = List.of(
                entry("572", "mal code wins"),
                entry("0", "performed PT and UT inspections"),
                entry("999", "general repair"));

        Map<String, LinkedList<LogEntry>> groupedEntries = InspectionMethod.groupEntriesByMethod(entries);

        assertEquals(1, groupedEntries.get("Eddy Current").size());
        assertEquals(1, groupedEntries.get("Liquid Penetrant").size());
        assertEquals(0, groupedEntries.get("Magnetic Particle").size());
        assertEquals(0, groupedEntries.get("Radiographic").size());
        assertEquals(1, groupedEntries.get("Ultrasonic").size());
        assertEquals(1, groupedEntries.get("Other").size());
    }

    private static LogEntry entry(String malCode, String correctiveAction) {
        return new LogEntry("2460325", "JANE DOE", "WING PANEL", malCode, "1.5", correctiveAction);
    }
}
