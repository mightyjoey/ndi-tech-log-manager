package org.example.m11techlogapp.model;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.example.m11techlogapp.LogEntry;

import java.io.*;
import java.sql.*;
import java.util.*;

import static org.example.m11techlogapp.DateUtils.fromDateTimeToJulian;

public class DBController {

    private ConnectDB connection;

    public DBController(ConnectDB connection) {
        this.connection = connection;
    }

    public void clearDB(){
        try(Connection conn = connection.getConnection()){
            String sql = "DELETE FROM worker_entry";
            Statement stm = conn.createStatement();
            stm.execute(sql);
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public String insertEntry(double dttm, String name, String nomen, String mal_cd, double hours, String corr_act){
            String sql = "INSERT INTO worker_entry (dttm, name, nomen, mal_cd, hours, corr_act) VALUES (?,?,?,?,?,?)";
            try (Connection conn = connection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                ps.setDouble(1, dttm);
                ps.setString(2, name);
                ps.setString(3, nomen);
                ps.setString(4, mal_cd);
                ps.setDouble(5, hours);
                ps.setString(6, corr_act);

                if (ps.executeUpdate() == 1) {
                    conn.commit();
                    return "update success";
                } else {
                    return "update failed";
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + e.getMessage();
            }
        }

    public String updateDB3(String filepath) {
        int totalLines = 0;
        int updatesMade = 0;

        try (Connection conn = connection.getConnection()) {
            conn.setAutoCommit(false);

            try (FileInputStream file = new FileInputStream(filepath);
                 Workbook workbook = WorkbookFactory.create(file)) {
                Sheet sheet = workbook.getSheetAt(0);

                for (Row row : sheet) {
                    // Skip empty rows
                    if (row == null || row.getPhysicalNumberOfCells() == 0) continue;

                    // Expecting exactly 6 columns
                    if (row.getPhysicalNumberOfCells() != 6) {
                        conn.rollback();
                        return "table row must have exactly 6 columns (date time, name, nomenclature, mal. code, hours, corrective action)";
                    }

                    // Extract cell values
                    double xdttm = fromDateTimeToJulian(getCellValueAsString(row.getCell(0)));
                    String xname = getCellValueAsString(row.getCell(1)).trim();
                    String xnomen = getCellValueAsString(row.getCell(2)).trim();
                    String xmal_cd = getCellValueAsString(row.getCell(3)).trim();
                    double xhours = Double.parseDouble(getCellValueAsString(row.getCell(4)));
                    String xcorr_act = getCellValueAsString(row.getCell(5));


                    String workerEntryInsert = "INSERT INTO worker_entry(dttm, name, nomen, mal_cd, hours, corr_act) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(workerEntryInsert)) {
                        ps.setDouble(1, xdttm);
                        ps.setString(2, xname);
                        ps.setString(3, xnomen);
                        ps.setString(4, xmal_cd);
                        ps.setDouble(5, xhours);
                        ps.setString(6, xcorr_act);

                        if (ps.executeUpdate() == 1) {
                            updatesMade++;
                        } else {
                            System.out.println("Failed to insert: " + xname + " " + xnomen + " " + xmal_cd);
                        }
                    }

                    totalLines++;
                }

                conn.commit();
            }

            System.out.println("Update Success: " + updatesMade + " entries updated out of " + totalLines);
            return "Update Success: " + updatesMade + " entries updated out of " + totalLines;

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    // Helper method to get cell value as String
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().toUpperCase();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) { // whole number
                        return String.valueOf((long) value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            default:
                return "";
        }
    }

    public ArrayList<String> selectDistinctName() {
        ArrayList<String> distinctNames = new ArrayList<>();
        String selectWorker = "SELECT DISTINCT name FROM worker_entry";

        try (Connection conn = connection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectWorker)) {

            while (rs.next()) {
                distinctNames.add(rs.getString(1));
            }
            return distinctNames;

        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
        }
        return distinctNames;
    }

    private String selectMethodConditions(String method) {
        return switch (method) {
            case "Eddy Current" -> "(mal_cd = '572' OR" +
                    " (corr_act like '% ET %' or corr_act like '%EDDY CURRENT%'))";
            case "Liquid Penetrant" -> "(mal_cd = '576' OR" +
                    " (corr_act like '% PT %' or corr_act like '%PENETRANT%'))";
            case "Magnetic Particle" -> "(mal_cd = '571' OR" +
                    " (corr_act like '% MT %' or corr_act like '%MAG PARTICLE%'))";
            case "Radiographic" -> "(mal_cd = '570' OR" +
                    " (corr_act like '% RT %' or corr_act like '%RADIOGRAPHIC%'))";
            case "Ultrasonic" -> "(mal_cd = '575' OR" +
                    " (corr_act like '% UT %' or corr_act like '%ULTRASONIC%'))";
            case "All Inspections" -> "(mal_cd IN ('570', '571', '572', '573', '575', '576', '579', '0'))";
            default -> null;
        };
    }

    public List<LogEntry> getWorkerEntries(String method, String distinctNames, double beginDate, double endDate) {
        List<LogEntry> logEntries = new ArrayList<>();

        String sql = "SELECT dttm, name, nomen, mal_cd, hours, corr_act " +
                "FROM worker_entry " +
                "WHERE name in " + distinctNames +
                " AND dttm BETWEEN " + beginDate + " AND " + endDate +
                " AND " + selectMethodConditions(method);

        System.out.println(sql);

        try (Connection conn = connection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            // Iterate over the ResultSet and create LogEntry objects
            while (rs.next()) {
                String date = rs.getString("dttm");
                String name = rs.getString("name");
                String nomen = rs.getString("nomen");
                String malCd = rs.getString("mal_cd");
                String hours = rs.getString("hours");
                String corrAct = rs.getString("corr_act");

                // Create a new LogEntry and add it to the list
                LogEntry logEntry = new LogEntry(date, name, nomen, malCd, hours, corrAct);
                logEntries.add(logEntry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logEntries;  // Return the list of log entries
    }

    public List<LogEntry> searchForKeyword(String keyword) {
        List<LogEntry> logEntries = new ArrayList<>();
        String keyNomen = "%" + keyword.toUpperCase() + "%";
        String keyCorr_act = "%"+  keyword.toUpperCase() + "%";

        try (Connection conn = connection.getConnection()) {
            conn.setAutoCommit(false);
            String sql = "SELECT * FROM worker_entry WHERE nomen LIKE ? or corr_act LIKE ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, keyNomen);
                ps.setString(2, keyCorr_act);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    String date = rs.getString("dttm");
                    String name = rs.getString("name");
                    String nomen = rs.getString("nomen");
                    String malCd = rs.getString("mal_cd");
                    String hours = rs.getString("hours");
                    String corrAct = rs.getString("corr_act");

                    // Create a new LogEntry and add it to the list
                    LogEntry logEntry = new LogEntry(date, name, nomen, malCd, hours, corrAct);
                    logEntries.add(logEntry);
                }

            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return logEntries;
    }

    public String deleteEntry(double dttm, String name, String nomen, String malCd, double hours, String corrAct) {
        try (Connection conn = connection.getConnection()) {
            conn.setAutoCommit(false);

            String sql = "DELETE FROM worker_entry " +
                    "WHERE dttm = ? AND name = ? AND nomen = ? " +
                    "AND mal_cd = ? AND hours = ? AND corr_act = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, dttm);
                ps.setString(2, name);
                ps.setString(3, nomen);
                ps.setString(4, malCd);
                ps.setDouble(5, hours);
                ps.setString(6, corrAct);

                if (ps.executeUpdate() == 1) {
                    conn.commit();
                    return "delete success";
                } else {
                    return "delete failed";
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
            return "ERROR:" + e.getMessage();
        }
    }

}
