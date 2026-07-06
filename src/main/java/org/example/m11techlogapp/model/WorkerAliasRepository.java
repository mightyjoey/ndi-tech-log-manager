package org.example.m11techlogapp.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WorkerAliasRepository {
    
    private ConnectDB connection;

    public WorkerAliasRepository(ConnectDB connection) {
        this.connection = connection;
    }

    public String addWorkerAlias(String full_name, String alias){
        String sql = "INSERT INTO worker_aliases(full_name, alias) VALUES (?, ?)";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){
            conn.setAutoCommit(false);
            ps.setString(1, full_name);
            ps.setString(2, alias);
            if (ps.executeUpdate() == 1){
                conn.commit();
                return "Worker alias added successfully.";
            } else {
                conn.rollback();
                return "Failed to add worker alias.";   
             }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    public String updateWorkerAliases(String full_name, List<String> aliases){
        String deleteSQL = "DELETE FROM worker_aliases WHERE full_name = ?";
        String insertSQL = "INSERT INTO worker_aliases(full_name, alias) VALUES (?, ?)";
        try (Connection conn = connection.getConnection();
             PreparedStatement deletePS = conn.prepareStatement(deleteSQL);
             PreparedStatement insertPS = conn.prepareStatement(insertSQL)){
            conn.setAutoCommit(false);
            deletePS.setString(1, full_name);
            deletePS.executeUpdate();
            for (String alias : aliases){
                insertPS.setString(1, full_name);
                insertPS.setString(2, alias);
                insertPS.addBatch();
            }
            insertPS.executeBatch();
            conn.commit();
            return "Worker aliases updated successfully.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    public String deleteWorkerAlias(String full_name, String alias) {
        String sql = "DELETE FROM worker_aliases WHERE full_name = ? AND alias = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){
            conn.setAutoCommit(false);
            ps.setString(1, full_name);
            ps.setString(2, alias);
            if (ps.executeUpdate() == 1){
                conn.commit();
                return "Worker alias deleted successfully.";
            } else {
                conn.rollback();
                return "Failed to delete worker alias.";   
             }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    public List<String> getAliasesForWorker(String full_name){
        List<String> aliases = new ArrayList<>();
        String sql = "SELECT alias FROM worker_aliases WHERE full_name = ?";
        try (Connection conn = connection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)){
                ps.setString(1, full_name);
                ResultSet rs = ps.executeQuery();
                while (rs.next()){
                    aliases.add(rs.getString("alias"));
                }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return aliases;
    }
}