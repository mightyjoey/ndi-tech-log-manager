package org.example.m11techlogapp.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class WorkerRepository {

    private ConnectDB connection;

    public WorkerRepository(ConnectDB connection) {
        this.connection = connection;
    }

    public String addWorker(String fullName) {
        String sql = "INSERT INTO workers (full_name) VALUES (?)";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, fullName);
            
            if (ps.executeUpdate() == 1){
                conn.commit();
                return "Worker added successfully.";
            } else {
                conn.rollback();
                return "Failed to add worker.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    public String deleteWorker(String fullName) {
        String sql = "DELETE FROM workers WHERE full_name = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, fullName);
            
            if (ps.executeUpdate() == 1){
                conn.commit();
                return "Worker deleted successfully.";
            } else {
                conn.rollback();
                return "Failed to delete worker.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    public ArrayList<String> getAllWorkers() {
        ArrayList<String> workers = new ArrayList<>();
        String sql = "SELECT full_name FROM workers";
        try (Connection conn = connection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                workers.add(rs.getString("full_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return workers;
    }

    
}
