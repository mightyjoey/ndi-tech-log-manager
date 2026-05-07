package org.example.m11techlogapp.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.MonthDay;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnualLogPeriodTest {

    @Test
    void startForUsesCurrentYearWhenEntryIsOnOrAfterSplitDate() {
        LocalDate periodStart = AnnualLogPeriod.startFor(
                LocalDate.of(2024, 5, 6),
                MonthDay.of(5, 1));

        assertEquals(LocalDate.of(2024, 5, 1), periodStart);
    }

    @Test
    void startForUsesPreviousYearWhenEntryIsBeforeSplitDate() {
        LocalDate periodStart = AnnualLogPeriod.startFor(
                LocalDate.of(2024, 4, 30),
                MonthDay.of(5, 1));

        assertEquals(LocalDate.of(2023, 5, 1), periodStart);
    }
}
