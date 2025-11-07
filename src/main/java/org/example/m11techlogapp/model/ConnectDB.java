package org.example.m11techlogapp.model;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectDB {
    private Connection conn;
    private static final String DB_FILE_PATH;
    private static final String DB_URL;

    static {
        String dbPath = null;
        try {
            // Locate the running JAR inside the .app bundle
            File jarFile = new File(ConnectDB.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            // Navigate up to the .app root
            File appRoot = jarFile.getParentFile().getParentFile();// Contents/Java → Contents
            if(appRoot.getName().equals("Contents")){
                File resourcesDir = new File(appRoot, "Resources");
                // Point to worker_entry.db inside Resources
                dbPath = new File(resourcesDir, "worker_entry.db").getAbsolutePath();
            } else{
                dbPath = System.getProperty("user.dir") + File.separator + "worker_entry.db";
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

//        private static final String DB_FILE_PATH = System.getProperty("user.dir") + File.separator + "worker_entry.db";
        DB_FILE_PATH = dbPath;
        DB_URL = "jdbc:sqlite:" + DB_FILE_PATH;
    }

    public ConnectDB() {
        try {
            System.out.println("Connecting to DB at: " + DB_FILE_PATH);
            conn = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return conn;
    }

    public void close() {
        try {
            if (conn != null) {
                conn.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}