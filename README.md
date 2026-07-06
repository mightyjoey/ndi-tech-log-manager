# M11TechLogApp

M11TechLogApp is a desktop application for managing nondestructive inspection (NDI) tech log records. It is built with Java 22, JavaFX, SQLite, Apache POI, and PDFBox.

The app lets users import or enter inspection records, search the database, generate filtered log views, and export completed NDI Tech Log and Detailed Tech Log PDFs.

> Current targets: macOS and Windows. Maven selects the JavaFX native artifacts for the build host automatically.

## Features

- Add individual inspection records
- Import inspection records from Excel workbooks
- Search records by nomenclature or corrective action keyword
- Manage workers and register alias name spellings that appear in the raw records
- Generate logs by inspector name, or by a worker and all of their aliases
- Filter generated logs by inspection method and date range
- Generate all-inspection logs grouped by method
- Optionally split generated logs by an annual anniversary date
- Review totals for inspections and hours before export
- Remove rows from generated results, with the option to delete the record from the database
- Export `NDI_Tech_Log.pdf` and `Detailed_Tech_Log.pdf`

## Requirements

- Windows 10/11 or macOS
- JDK 22
- Maven, or the included Maven wrapper
- SQLite database file named `worker_entry.db`

## Quick Start

From the project root:

```bash
./mvnw javafx:run
```

On Windows:

```powershell
.\mvnw.cmd javafx:run
```

If your local Maven installation is configured for Java 22, this also works:

```bash
mvn javafx:run
```

The app expects a writable `worker_entry.db` in the project root when run from source. If the file does not exist, the app creates the `worker_entry` table automatically, but it will start empty.

## Running in IntelliJ IDEA

1. Open the project in IntelliJ IDEA.
2. Confirm the project SDK is JDK 22.
3. Open the Maven tool window.
4. Run `Plugins > javafx > javafx:run`.

## Packaging for macOS

Build the shaded JAR:

```bash
./mvnw clean package
```

Create a macOS app image:

```bash
jpackage \
  --type app-image \
  --name M11TechLogApp \
  --input target \
  --main-jar M11TechLogApp-shaded.jar \
  --main-class org.example.m11techlogapp.Launcher \
  --icon MyIcon.icns \
  --dest .
```

The Maven build copies `src/main/jpackage/worker_entry.db` into `target`, so `jpackage --input target` includes the starter database in the app image.

## Packaging for Windows

Build the shaded JAR:

```powershell
.\mvnw.cmd clean package
```

Create a Windows app image:

```powershell
jpackage `
  --type app-image `
  --name M11TechLogApp `
  --input target `
  --main-jar M11TechLogApp-shaded.jar `
  --main-class org.example.m11techlogapp.Launcher `
  --dest .
```

The Windows build uses JavaFX `win` artifacts automatically. Add `--icon path\to\icon.ico` if a Windows `.ico` icon is available.

## Database

The app uses SQLite. Inspection records are stored in the `worker_entry` table, while worker identities and their alias name spellings are stored in the `workers` and `worker_aliases` tables. Foreign keys are enforced — every connection runs `PRAGMA foreign_keys = ON`.

When running from source, the database path is:

```text
worker_entry.db
```

When running from a packaged `.app`, the app looks for:

```text
~/Library/Application Support/M11TechLogApp/worker_entry.db
```

When running from a packaged Windows app image, the app looks for:

```text
%APPDATA%\M11TechLogApp\worker_entry.db
```

If `%APPDATA%` is unavailable, it falls back to:

```text
%LOCALAPPDATA%\M11TechLogApp\worker_entry.db
```

On first launch, the packaged app copies the starter database from the app bundle or Windows app image into the writable user data location. This avoids writing records into platform install directories, which may be read-only after download or installation.

### Schema

Inspection records:

```sql
CREATE TABLE IF NOT EXISTS worker_entry (
  dttm REAL NOT NULL,
  name TEXT NOT NULL,
  nomen TEXT NOT NULL,
  mal_cd TEXT NOT NULL,
  hours REAL NOT NULL,
  corr_act TEXT NOT NULL,
  PRIMARY KEY (dttm, name, nomen, mal_cd, hours, corr_act) ON CONFLICT IGNORE
);
```

Duplicate rows are ignored by the primary key constraint.

Workers and their alias name spellings:

```sql
CREATE TABLE IF NOT EXISTS workers (
  full_name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS worker_aliases (
  full_name TEXT NOT NULL,
  alias TEXT NOT NULL,
  FOREIGN KEY (full_name) REFERENCES workers (full_name) ON DELETE CASCADE,
  UNIQUE (full_name, alias)
);
```

A worker's `full_name` is the canonical identity; each `alias` is a name spelling as it appears in the `name` column of `worker_entry`. Deleting a worker cascades to remove their aliases. All three tables are created automatically on first launch if they do not exist.

## Excel Import Format

Use the Update Records screen to import an Excel workbook. The first sheet is read, and every non-empty row must contain exactly six columns with no header row.

| Column | Field | Example | Notes |
| --- | --- | --- | --- |
| 1 | Date/time | `3/9/2023 11:03` | Excel date cells are recommended. Text dates should use `M/d/yyyy H:mm`. |
| 2 | Last name | `SMITH` | Text is converted to uppercase. |
| 3 | Nomenclature | `WHEEL BRAKE` | Text is converted to uppercase. |
| 4 | Malfunction code | `572` | Inspection method code. |
| 5 | Hours | `5.6` | Numeric value. |
| 6 | Corrective action | `INSPECTED BRAKE` | Text is converted to uppercase. |

Supported method codes used by the app:

| Code | Method |
| --- | --- |
| `570` | Radiographic |
| `571` | Magnetic Particle |
| `572` | Eddy Current |
| `575` | Ultrasonic |
| `576` | Liquid Penetrant |
| `579` | Other |
| `0` | Other/unspecified |

The importer uses Apache POI to detect workbook format automatically, so both `.xls` and `.xlsx` files are supported.

## Managing Workers and Aliases

The same inspector can appear under several name spellings in imported records (for example `SMITH`, `SMITH J`, `J SMITH`). Workers let you group those spellings under one canonical identity so logs can be generated for a person rather than a single spelling.

1. Open Manage Workers.
2. Add a worker by entering a full name and creating it. This becomes the canonical identity.
3. Select a worker to view and edit their aliases.
4. Assign aliases by picking the record names (drawn from the distinct `name` values in the database) that belong to that worker.

When generating logs you can then select the worker, and the app queries every record matching any of their aliases.

## Generating and Exporting Logs

1. Open Generate Logs.
2. Select one or more inspector names, or enable the worker option to pick a worker and use all of their aliases.
3. Choose an inspection method or All Inspections.
4. Choose a beginning and ending date.
5. Optionally enable Annual Split and choose the anniversary date.
6. Review the generated table and totals.
7. Export PDFs by selecting a parent folder and entering a new output folder name.

Exports create:

```text
NDI_Tech_Log.pdf
Detailed_Tech_Log.pdf
```

For All Inspections, the export groups records into method-specific pages. For annual split exports, pages are broken at the selected anniversary date.

## Project Layout

```text
src/main/java/org/example/m11techlogapp/
  Launcher.java                          Packaged app entry point
  HelloApplication.java                  JavaFX application startup
  controller/                            JavaFX screen controllers
  model/                                 Connection, repositories, and domain types
  service/                               Log query and PDF export services
  util/                                  Date and annual-period helpers

src/main/resources/
  org/example/m11techlogapp/*.fxml       JavaFX views
  org/example/m11techlogapp/app.css      App styling
  ndiLog.pdf                             NDI Tech Log template
  detailedLog.pdf                        Detailed Tech Log template

src/main/jpackage/
  worker_entry.db                        Starter database copied during packaging
```

## Development Notes

- Main class for packaged runs: `org.example.m11techlogapp.Launcher`
- Maven artifact: `M11TechLogApp`
- Shaded JAR output: `target/M11TechLogApp-shaded.jar`
- Local runtime databases and packaged build outputs are ignored by Git.
- JavaFX classifiers are selected with Maven profiles: `mac-aarch64`, `mac-x64`, `windows`, and `linux`.
- Run the JUnit test suite with `./mvnw test` (repository, service, integration, and system tests live under `src/test/java`).
