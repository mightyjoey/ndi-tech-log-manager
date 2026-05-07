package org.example.m11techlogapp.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateUtilsTest {

    @Test
    void fromDateToJulianConvertsKnownDates() {
        assertEquals(2451545, DateUtils.fromDateToJulian(LocalDate.of(2000, 1, 1)));
        assertEquals(2440588, DateUtils.fromDateToJulian(LocalDate.of(1970, 1, 1)));
        assertEquals(2460311, DateUtils.fromDateToJulian(LocalDate.of(2024, 1, 1)));
    }

    @Test
    void fromJulianToDateStringConvertsKnownJulianDates() {
        assertEquals("2000-01-01", DateUtils.fromJulianToDateString("2451545.0"));
        assertEquals("1970-01-01", DateUtils.fromJulianToDateString("2440588"));
        assertEquals("2024-01-01", DateUtils.fromJulianToDateString("2460311"));
    }

    @Test
    void fromJulianToDateStringRejectsNonNumericInput() {
        assertThrows(NumberFormatException.class, () -> DateUtils.fromJulianToDateString("not-a-date"));
    }

    @Test
    void fromDateTimeToJulianParsesSupportedInputFormats() {
        assertEquals(2460324.5, DateUtils.fromDateTimeToJulian("1/15/2024 7:30"));
        assertEquals(2460324.5, DateUtils.fromDateTimeToJulian("1/15/2024 7:30:05:123"));
        assertEquals(2460324.5, DateUtils.fromDateTimeToJulian("Mon Jan 15 07:30:05 PST 2024"));
    }
}
