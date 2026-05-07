package org.example.m11techlogapp.util;

import java.time.LocalDate;
import java.time.MonthDay;

public final class AnnualLogPeriod {
    private AnnualLogPeriod() {
    }

    public static LocalDate startFor(LocalDate entryDate, MonthDay splitDate) {
        LocalDate periodStart = splitDate.atYear(entryDate.getYear());
        if (entryDate.isBefore(periodStart)) {
            periodStart = splitDate.atYear(entryDate.getYear() - 1);
        }
        return periodStart;
    }
}
