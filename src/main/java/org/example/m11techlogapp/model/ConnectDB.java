package org.example.m11techlogapp.model;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectDB {
    private Connection conn;
    private static final String DB_FILE_PATH;
    private static final String DB_URL;

    static {
        try {
            DB_FILE_PATH = resolveDatabasePath();
        } catch (IOException | URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }

        DB_URL = "jdbc:sqlite:" + DB_FILE_PATH;
    }

    public ConnectDB() {
        try {
            System.out.println("Connecting to DB at: " + DB_FILE_PATH);
            conn = DriverManager.getConnection(DB_URL);
            ensureDatabaseInitialized();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return conn;
    }

    private static String resolveDatabasePath() throws IOException, URISyntaxException {
        File appLocation = new File(ConnectDB.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());

        File contentsDir = appLocation.getParentFile() == null ? null : appLocation.getParentFile().getParentFile();
        if (contentsDir != null && "Contents".equals(contentsDir.getName())) {
            Path userDatabase = Path.of(
                    System.getProperty("user.home"),
                    "Library",
                    "Application Support",
                    "M11TechLogApp",
                    "worker_entry.db"
            );

            if (Files.notExists(userDatabase)) {
                Files.createDirectories(userDatabase.getParent());

                Path bundledDatabase = contentsDir.toPath().resolve("Resources").resolve("worker_entry.db");
                Path bundledJavaDatabase = contentsDir.toPath().resolve("Java").resolve("worker_entry.db");

                if (Files.exists(bundledDatabase)) {
                    Files.copy(bundledDatabase, userDatabase);
                } else if (Files.exists(bundledJavaDatabase)) {
                    Files.copy(bundledJavaDatabase, userDatabase);
                }
            }

            return userDatabase.toString();
        }

        return System.getProperty("user.dir") + File.separator + "worker_entry.db";
    }

    private void ensureDatabaseInitialized() throws SQLException {
        String createWorkerEntryTable = """
                CREATE TABLE IF NOT EXISTS worker_entry (
                    dttm REAL not null,
                    name TEXT not null,
                    nomen TEXT not null,
                    mal_cd TEXT not null,
                    hours REAL not null,
                    corr_act TEXT not null,
                    constraint worker_entries_pk primary key (
                        dttm,
                        name,
                        nomen,
                        mal_cd,
                        hours,
                        corr_act
                    ) on conflict ignore
                )
                """;

        try (var statement = conn.createStatement()) {
            statement.execute(createWorkerEntryTable);
        }
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
