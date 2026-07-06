package org.example.m11techlogapp.model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;

public class ConnectDB {
    private static final String APP_NAME = "M11TechLogApp";
    private static final String DB_FILE_NAME = "worker_entry.db";
    private Connection conn;
    private final String dbUrl;
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
        this(DB_URL);
    }

    ConnectDB(String dbUrl) {
        this.dbUrl = dbUrl;
        try {
            System.out.println("Connecting to DB at: " + dbUrl);
            conn = DriverManager.getConnection(dbUrl);
            enableForeignKeys(conn);
            ensureDatabaseInitialized();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(dbUrl);
            enableForeignKeys(connection);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void enableForeignKeys(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }

    private static String resolveDatabasePath() throws IOException, URISyntaxException {
        Path appLocation = Path.of(ConnectDB.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());

        Path macContentsDir = getMacAppContentsDirectory(appLocation);
        if (macContentsDir != null) {
            Path userDatabase = getUserDataDirectory().resolve(DB_FILE_NAME);
            copyStarterDatabaseIfNeeded(userDatabase,
                    macContentsDir.resolve("Resources").resolve(DB_FILE_NAME),
                    macContentsDir.resolve("Java").resolve(DB_FILE_NAME));

            return userDatabase.toString();
        }

        Path windowsAppDir = getWindowsAppDirectory(appLocation);
        if (windowsAppDir != null) {
            Path userDatabase = getUserDataDirectory().resolve(DB_FILE_NAME);
            copyStarterDatabaseIfNeeded(userDatabase, windowsAppDir.resolve(DB_FILE_NAME));

            return userDatabase.toString();
        }

        return Path.of(System.getProperty("user.dir"), DB_FILE_NAME).toString();
    }

    private static Path getMacAppContentsDirectory(Path appLocation) {
        Path parent = appLocation.getParent();
        Path contentsDir = parent == null ? null : parent.getParent();
        if (contentsDir != null && "Contents".equals(contentsDir.getFileName().toString())) {
            return contentsDir;
        }

        return null;
    }

    private static Path getWindowsAppDirectory(Path appLocation) {
        if (!isWindows()) {
            return null;
        }

        Path appDir = Files.isDirectory(appLocation) ? appLocation : appLocation.getParent();
        if (appDir != null && "app".equalsIgnoreCase(appDir.getFileName().toString())) {
            return appDir;
        }

        return null;
    }

    private static Path getUserDataDirectory() {
        if (isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, APP_NAME);
            }

            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                return Path.of(localAppData, APP_NAME);
            }
        }

        if (isMac()) {
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", APP_NAME);
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome != null && !xdgDataHome.isBlank()) {
            return Path.of(xdgDataHome, APP_NAME);
        }

        return Path.of(System.getProperty("user.home"), "." + APP_NAME);
    }

    private static void copyStarterDatabaseIfNeeded(Path userDatabase, Path... starterDatabases) throws IOException {
        if (Files.exists(userDatabase)) {
            return;
        }

        Files.createDirectories(userDatabase.getParent());

        for (Path starterDatabase : starterDatabases) {
            if (Files.exists(starterDatabase)) {
                Files.copy(starterDatabase, userDatabase);
                return;
            }
        }
    }

    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
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

            String createWorkerTable = """
                CREATE TABLE IF NOT EXISTS workers (
                    full_name TEXT NOT NULL UNIQUE
                )
                """;
                
            String createWorkerAliasTable = """
                CREATE TABLE IF NOT EXISTS worker_aliases (
                    full_name TEXT NOT NULL,
                    alias TEXT NOT NULL,
                    FOREIGN KEY (full_name) REFERENCES workers (full_name) ON DELETE CASCADE,
                    UNIQUE (full_name, alias)
                )   
                    """;
            
        try (var statement = conn.createStatement()) {
            statement.execute(createWorkerEntryTable);
            statement.execute(createWorkerTable);
            statement.execute(createWorkerAliasTable);
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
