package org.example.m11techlogapp.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public enum InspectionMethod {
    EDDY_CURRENT("Eddy Current", "572", List.of(" EDDY CURRENT", " ET ")),
    LIQUID_PENETRANT("Liquid Penetrant", "576", List.of(" PENETRANT", " PT ")),
    MAGNETIC_PARTICLE("Magnetic Particle", "571", List.of(" MAG PARTICLE", " MT ")),
    RADIOGRAPHIC("Radiographic", "570", List.of(" RADIOGRAPHIC", " RT ")),
    ULTRASONIC("Ultrasonic", "575", List.of(" ULTRASONIC", " UT ")),
    OTHER("Other", "579", List.of());

    public static final String ALL_INSPECTIONS = "All Inspections";
    public static final String ZERO_CODE = "0";

    private final String displayName;
    private final String malCode;
    private final List<String> correctiveActionMarkers;

    InspectionMethod(String displayName, String malCode, List<String> correctiveActionMarkers) {
        this.displayName = displayName;
        this.malCode = malCode;
        this.correctiveActionMarkers = correctiveActionMarkers;
    }

    public String malCode() {
        return malCode;
    }

    public List<String> correctiveActionLikePatterns() {
        return correctiveActionMarkers.stream()
                .map(marker -> marker.endsWith(" ") ? "%" + marker + "%" : "%" + marker.trim() + "%")
                .toList();
    }

    public static List<String> logMethodOptions() {
        List<String> options = inspectionDisplayNames();
        options.add(ALL_INSPECTIONS);
        return options;
    }

    public static List<String> recordMethodOptions() {
        List<String> options = inspectionDisplayNames();
        options.add(ZERO_CODE);
        options.add(OTHER.displayName);
        return options;
    }

    public static boolean isAllInspections(String method) {
        return ALL_INSPECTIONS.equals(method);
    }

    public static String malCodeForDisplayName(String method) {
        InspectionMethod inspectionMethod = fromDisplayName(method);
        return inspectionMethod == null ? ZERO_CODE : inspectionMethod.malCode;
    }

    public static InspectionMethod fromDisplayName(String method) {
        for (InspectionMethod inspectionMethod : values()) {
            if (inspectionMethod.displayName.equals(method)) {
                return inspectionMethod;
            }
        }
        return null;
    }

    public static List<String> methodNamesForEntry(LogEntry entry) {
        for (InspectionMethod inspectionMethod : inspectionMethods()) {
            if (inspectionMethod.malCode.equals(entry.getMal_cd())) {
                return List.of(inspectionMethod.displayName);
            }
        }

        return methodNamesFromCorrectiveAction(entry.getCorr_act());
    }

    public static List<String> methodNamesFromCorrectiveAction(String correctiveAction) {
        List<String> methods = new ArrayList<>();
        String text = correctiveAction == null ? "" : " " + correctiveAction.toUpperCase() + " ";

        for (InspectionMethod inspectionMethod : inspectionMethods()) {
            for (String marker : inspectionMethod.correctiveActionMarkers) {
                if (text.contains(marker)) {
                    methods.add(inspectionMethod.displayName);
                    break;
                }
            }
        }

        if (methods.isEmpty()) {
            methods.add(OTHER.displayName);
        }

        return methods;
    }

    public static Map<String, LinkedList<LogEntry>> groupEntriesByMethod(List<LogEntry> entries) {
        Map<String, LinkedList<LogEntry>> groupedEntries = new LinkedHashMap<>();
        for (InspectionMethod inspectionMethod : values()) {
            groupedEntries.put(inspectionMethod.displayName, new LinkedList<>());
        }

        for (LogEntry entry : entries) {
            for (String methodName : methodNamesForEntry(entry)) {
                groupedEntries.get(methodName).add(entry);
            }
        }

        return groupedEntries;
    }

    private static List<String> inspectionDisplayNames() {
        List<String> displayNames = new ArrayList<>();
        for (InspectionMethod inspectionMethod : inspectionMethods()) {
            displayNames.add(inspectionMethod.displayName);
        }
        return displayNames;
    }

    private static List<InspectionMethod> inspectionMethods() {
        return List.of(EDDY_CURRENT, LIQUID_PENETRANT, MAGNETIC_PARTICLE, RADIOGRAPHIC, ULTRASONIC);
    }
}
