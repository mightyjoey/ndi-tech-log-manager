# M11TechLogApp

**M11TechLogApp** is a desktop application written in **Java** using **JavaFX** for the user interface and **SQLite** for the database. It is designed to manage nondestructive inspection (NDI) logs, allowing users to add, edit, upload, and export inspection records.  

**Currently macOS only. Windows support is planned for a future release.**

---

## Features

- Add, edit, and remove inspection entries  
- Upload inspection data from Excel (`.xlsx`) files to update the database  
- Generate PDF reports (NDI Tech Log & Detailed Tech Log)  
- Track total inspections and hours  
- User-friendly JavaFX interface  

---

## Options for Installation / Running the App

### macOS Standalone

1. Copy the `M11TechLogApp.app` folder to your Desktop or any preferred location.  
2. Double-click the `.app` to launch the application.  

### IntelliJ IDEA

1. Open the project in **IntelliJ IDEA**.  
2. Open the **Maven** tool window (usually on the right side).  
3. Expand **Plugins → javafx → javafx:run**.  
4. Click **Run** to start the application.

### Using the Terminal
1. Open a terminal in the project root (where the pom.xml is).
2. Run the following command: mvn javafx:run

Note: Make sure your Maven and Java versions match the pom.xml configuration (Java 22, JavaFX 22.0.1

---

## Database

- The app uses an **SQLite** database (`worker_entry.db`) stored in the `Resources` folder inside the `.app` bundle.  
- When running in IntelliJ IDEA, place the database in the `resources` folder at the project root.  

**Database connection logic** is handled in `ConnectDB.java`. The app opens and closes the connection for each query. This is sufficient for a small SQLite desktop app.  

---

## Uploading Excel Files

The app can update the database from Excel (`.xlsx`) files.  

### Column Layout (No Header)

| Column | Field              | Description                       | Example                 | Notes                                  |
|--------|--------------------|-----------------------------------|-------------------------|---------------------------------------|
| 1      | Date/Time          | Date and time of inspection       | 3/9/2023 11:03:59       | Must follow `M/d/yyyy HH:mm:ss` format |
| 2      | Last Name          | Inspector's last name             | SMITH                   | Text, all caps recommended            |
| 3      | Nomenclature       | Item being inspected              | WHEEL BRAKE             | Text                                   |
| 4      | Malfunction Code   | 3-digit code                      | 572                     | Exactly 3 digits                        |
| 5      | Hours              | Duration of inspection            | 5.6                     | Only 1 decimal allowed                  |
| 6      | Corrective Action  | Summary of what was done          | INSPECTED BRAKE         | Text                                   |

**Important Notes:**  

- No header row should be included.  
- No extra columns or empty rows.  
- Date/Time must strictly follow `M/d/yyyy HH:mm:ss` format (e.g., `3/9/2023 11:03:59`).  

**Example Row:**
3/9/2023 11:03:59 | SMITH | WHEEL BRAKE | 572 | 5.6 | inspected brake 

---

## Exporting PDFs

- Generate **NDI Tech Log** and **Detailed Tech Log** reports.  
- Select the output folder and provide a folder name for saving the PDFs.  
- Reports are automatically filled based on the current table data.  
