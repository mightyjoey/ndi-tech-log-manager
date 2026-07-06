package org.example.m11techlogapp.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void addWorkerPersistsWorkerAndReturnsSuccess() {
        WorkerRepository repository = repositoryForTempDatabase();

        String result = repository.addWorker("JANE DOE");

        assertEquals("Worker added successfully.", result);
        assertTrue(repository.getAllWorkers().contains("JANE DOE"));
    }

    @Test
    void addWorkerRejectsDuplicateNameBecauseOfUniqueConstraint() {
        WorkerRepository repository = repositoryForTempDatabase();
        repository.addWorker("JANE DOE");

        String result = repository.addWorker("JANE DOE");

        assertTrue(result.startsWith("ERROR:"), "expected error but was: " + result);
        assertEquals(1, repository.getAllWorkers().size());
    }

    @Test
    void deleteWorkerRemovesExistingWorker() {
        WorkerRepository repository = repositoryForTempDatabase();
        repository.addWorker("JANE DOE");

        String result = repository.deleteWorker("JANE DOE");

        assertEquals("Worker deleted successfully.", result);
        assertFalse(repository.getAllWorkers().contains("JANE DOE"));
    }

    @Test
    void deleteWorkerReportsFailureWhenWorkerMissing() {
        WorkerRepository repository = repositoryForTempDatabase();

        String result = repository.deleteWorker("NOBODY");

        assertEquals("Failed to delete worker.", result);
    }

    @Test
    void getAllWorkersReturnsEveryStoredWorker() {
        WorkerRepository repository = repositoryForTempDatabase();
        repository.addWorker("JANE DOE");
        repository.addWorker("JOHN SMITH");

        ArrayList<String> workers = repository.getAllWorkers();

        assertEquals(2, workers.size());
        assertTrue(workers.contains("JANE DOE"));
        assertTrue(workers.contains("JOHN SMITH"));
    }

    @Test
    void getAllWorkersIsEmptyForFreshDatabase() {
        WorkerRepository repository = repositoryForTempDatabase();

        assertTrue(repository.getAllWorkers().isEmpty());
    }

    private WorkerRepository repositoryForTempDatabase() {
        Path dbPath = tempDir.resolve("worker_entry.db");
        ConnectDB connectDB = new ConnectDB("jdbc:sqlite:" + dbPath);
        return new WorkerRepository(connectDB);
    }
}
