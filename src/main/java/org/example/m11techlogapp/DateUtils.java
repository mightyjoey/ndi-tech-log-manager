package org.example.m11techlogapp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class DateUtils {
    /**
     * Converts a Julian date string (Julian Day Number + fraction) into a
     * Gregorian calendar date string formatted as {@code yyyy-MM-dd}.
     *
     * <p>Example: Julian day "2451545.0" → "2000-01-01".</p>
     *
     * @param julianDateString the Julian date as a string
     *                         (e.g., "2451545.0").
     * @return the corresponding Gregorian date formatted as
     *         {@code yyyy-MM-dd}.
     * @throws NumberFormatException if the input cannot be parsed as a double
     */
    public static String fromJulianToDateString(String julianDateString) {
        double julianDate = Double.parseDouble(julianDateString);
        double J = julianDate + 0.5;

        int Z = (int) Math.floor(J);
        double F = J - Z;

        int A;
        if (Z < 2299161) {
            A = Z;
        } else {
            int alpha = (int) ((Z - 1867216.25) / 36524.25);
            A = Z + 1 + alpha - alpha / 4;
        }

        int B = A + 1524;
        int C = (int) ((B - 122.1) / 365.25);
        int D = (int) (365.25 * C);
        int E = (int) ((B - D) / 30.6001);

        double dayDecimal = B - D - Math.floor(30.6001 * E) + F;
        int day = (int) dayDecimal;
        double fractionalDay = dayDecimal - day;

        int month = (E < 14) ? (E - 1) : (E - 13);
        int year = (month > 2) ? (C - 4716) : (C - 4715);

        int hour = (int) (fractionalDay * 24);
        int minute = (int) ((fractionalDay * 24 - hour) * 60);

        LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return dateTime.format(formatter);
    }

    /**
     * Converts a {@link LocalDate} into its corresponding Julian Day Number (JDN).
     *
     * <p>The Julian Day Number is a continuous count of days since
     * January 1, 4713 BCE. This method adjusts the epoch day
     * (days since 1970-01-01) with the Julian offset (2440588).</p>
     *
     * <p>Example: 2000-01-01 → 2451545</p>
     *
     * @param localDate the {@code LocalDate} to convert
     * @return the Julian Day Number as a {@code long}
     */
    public static long fromDateToJulian(LocalDate localDate) {
        long epochDay = localDate.toEpochDay();
        return epochDay + 2440588;
    }

    public static double fromDateTimeToJulian(String datetimeString) {
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("M/d/yyyy H:mm");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss:SSS");
        DateTimeFormatter excelFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

        LocalDate date;

        try {
            date = LocalDate.parse(datetimeString, formatter1);
        } catch (DateTimeParseException e1) {
            try {
                date = LocalDate.parse(datetimeString, formatter2);
            } catch (DateTimeParseException e2) {
                ZonedDateTime zdt = ZonedDateTime.parse(datetimeString, excelFormatter);
                date = zdt.toLocalDate();
            }
        }

        int Y = date.getYear();
        int M = date.getMonthValue();
        int D = date.getDayOfMonth();

        if (M <= 2) {
            Y -= 1;
            M += 12;
        }

        int A = Y / 100;
        int B = 2 - A + (A / 4);

        return Math.floor(365.25 * (Y + 4716))
                + Math.floor(30.6001 * (M + 1))
                + D + B - 1524.5;
    }
}
