package org.example.m11techlogapp.service;

import org.example.m11techlogapp.model.InspectionMethod;
import org.example.m11techlogapp.model.LogEntry;
import org.example.m11techlogapp.model.ConnectDB;
import org.example.m11techlogapp.model.LogEntryRepository;

import java.time.LocalDate;

import static org.example.m11techlogapp.util.DateUtils.fromDateToJulian;

public class LogEntryService {

    public AddRecordResult addRecord(
            LocalDate date,
            String name,
            String nomen,
            String method,
            double hours,
            String correctiveAction) {

        double julianDate = fromDateToJulian(date);
        String normalizedName = name.toUpperCase();
        String normalizedNomen = nomen.toUpperCase();
        String malCode = InspectionMethod.malCodeForDisplayName(method);
        String normalizedCorrectiveAction = correctiveAction.toUpperCase();

        ConnectDB connectDB = new ConnectDB();
        LogEntryRepository logEntryRepository = new LogEntryRepository(connectDB);
        String message = logEntryRepository.insertEntry(
                julianDate,
                normalizedName,
                normalizedNomen,
                malCode,
                hours,
                normalizedCorrectiveAction);
        connectDB.close();

        LogEntry entry = null;
        if ("update success".equals(message)) {
            entry = new LogEntry(
                    String.valueOf(julianDate),
                    normalizedName,
                    normalizedNomen,
                    malCode,
                    String.valueOf(hours),
                    normalizedCorrectiveAction);
        }

        return new AddRecordResult(message, entry);
    }

    public record AddRecordResult(String message, LogEntry entry) {
        public boolean isSuccess() {
            return "update success".equals(message);
        }
    }
}
